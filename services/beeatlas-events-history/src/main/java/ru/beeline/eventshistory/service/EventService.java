/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.eventshistory.domain.Config;
import ru.beeline.eventshistory.dto.EventDTO;
import ru.beeline.eventshistory.exception.ValidationException;
import ru.beeline.eventshistory.repository.ConfigRepository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class EventService {
    @Value("${queue.notification.name}")
    private String notificationQueue;

    @Autowired
    private DataSource dataSource;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ConfigRepository configRepository;
    @Autowired
    private RabbitService rabbitService;

    @Transactional
    public void createAndSendEvents() {
        LocalDateTime startScanTime = LocalDateTime.now();
        log.info("ℹ️ Start create and send events method. start Scan Time: {}", startScanTime);
        List<Config> configs = configRepository.findAll()
                .stream()
                .filter(config -> config.getLastScanDate() != null)
                .toList();
        log.info("List configs size, LastScanDate != null: " + configs.size());
        configs.forEach(config -> {
            createAndSendEventsForConfig(config, startScanTime); // отдельная транзакция
        });
        log.info("ℹ️ updateConfigsWithNullScanDate.");

        updateConfigsWithNullScanDate();

        log.info("ℹ️ Create and send events method completed.");
    }

    public void createAndSendEventsForConfig(Config config, LocalDateTime startScanTime) {
        createEvents(config, startScanTime);
        configRepository.save(config);
    }

    public void updateConfigsWithNullScanDate() {
        configRepository.updateLastScanDateForNull(LocalDateTime.now());
    }

    public void createEvents(Config config, LocalDateTime startScanTime) {
        log.info("createEvents: " + config.toString());
        fetchData(config, startScanTime).forEach(it -> sendProcessing(it,
                config.getEntityType(),
                config.getNotificationRequired(),
                config.getClarificationEvent()));
        config.setLastScanDate(startScanTime);
        log.info("setLastScanDate: " + startScanTime);

    }

    private void sendProcessing(ObjectNode event, String entityType, Boolean notificationRequired, String clarificationEvent) {
        if (notificationRequired) {
            log.info("notification Required: true, sending a message to the notification Queue");
            rabbitService.sendMessage(event.deepCopy(), notificationQueue, entityType, clarificationEvent);
        }
        rabbitService.sendEvent(event, entityType);
    }

    public List<ObjectNode> fetchData(Config config, LocalDateTime startScanTime) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<ObjectNode> events = new ArrayList<>();
        String query;
        try {
            if (config.getTargetKey() != null && config.getTargetValue() != null) {
                query = createTargetSql(config);
            } else {
                query = createSql(config);
            }
            jdbcTemplate.query(query, (rs, rowNum) -> {
                Long id = rs.getLong(config.getIdAlias() == null || config.getIdAlias().equals("") ? "id" : config.getIdAlias());
                String columnName = null;
                String value = null;
                if (config.getGetName()) {
                    columnName = config.getNameAlias();
                    value = rs.getString(columnName);
                }
                LocalDateTime createdDate = rs.getObject(config.getCreatedDateAlias(), LocalDateTime.class);
                LocalDateTime updatedDate = config.getGenerateUpdateEvent() ? null : rs.getObject(config.getUpdatedDateAlias(),
                        LocalDateTime.class);
                LocalDateTime deletedDate = config.getGenerateDeleteEvent() ? null : rs.getObject(config.getDeletedDateAlias(),
                        LocalDateTime.class);
                String source = config.getGetSource() ? rs.getString("source") : null;
                Map<String, LocalDateTime> dates = new LinkedHashMap<>();
                if (createdDate != null && createdDate.isAfter(config.getLastScanDate().minusSeconds(10))
                        && createdDate.isBefore(startScanTime.minusSeconds(10))) {
                    dates.put("CREATE", createdDate);
                    log.info("creating event: " + config.getSchemaName() + "." + config.getTableName() + " created "
                            + "date: " + createdDate);
                } else {
                    if (updatedDate != null && updatedDate.isAfter(config.getLastScanDate()
                            .minusSeconds(10)) && updatedDate.isBefore(
                            startScanTime.minusSeconds(10))) {
                        dates.put("UPDATE", updatedDate);
                        log.info("creating event: " + config.getSchemaName() + "." + config.getTableName() + " updated "
                                + "date: " + updatedDate);
                    }
                }
                if (deletedDate != null && deletedDate.isAfter(config.getLastScanDate().minusSeconds(10))
                        && deletedDate.isBefore(startScanTime.minusSeconds(10))) {
                    dates.put("DELETE", deletedDate);
                    log.info("creating event: " + config.getSchemaName() + "." + config.getTableName() + " deleted "
                            + "date: " + deletedDate);
                }
                for (Map.Entry<String, LocalDateTime> date : dates.entrySet()) {
                    ObjectNode node = objectMapper.valueToTree(new EventDTO(id, date.getKey(), date.getValue(), source,
                            config.getChildrenIdAlias() == null ? null : rs.getString(config.getChildrenIdAlias())));
                    if (!config.getGetSource()) {
                        node.remove("source");
                    }
                    if (config.getGetName()) {
                        node.put("name", value);
                    }
                    events.add(node);
                }
                return null;
            });
            events.sort(Comparator.comparing(node -> node.get("lastUpdateDate").asText()));
        } catch (BadSqlGrammarException e) {
            log.info("❌ Ошибка SQL запроса к БД");
            log.info(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error executing query for schema: " + config.getSchemaName() + ", table: " + config.getTableName(),
                    e);
            return events;
        }
        return events;
    }

    public String createTargetSql(Config config) {
        log.info("ℹ️ Start method createTargetSql");
        validateTargetKeyType(config);
        log.info("ℹ️TargetValue = {}, TargetKeyType = {}, TargetKey = {}.", config.getTargetValue(), config.getTargetKeyType()
                , config.getTargetKey());
        String targetValue = config.getTargetValue();
        String targetKeyType = config.getTargetKeyType();
        if (targetKeyType.equals("text")) {
            targetValue = String.format("'%s'", config.getTargetValue());
        }
        String sqlRow = "SELECT ";
        sqlRow = sqlRow.concat(config.getIdAlias() == null || config.getIdAlias()
                .equals("") ? "id" : config.getIdAlias() + " ");
        sqlRow = sqlRow.concat(config.getGetName() ? ", " + config.getNameAlias() : "");
        sqlRow = sqlRow.concat(", " + config.getCreatedDateAlias());
        sqlRow = sqlRow.concat(config.getGetSource() ? ", source" : "");
        sqlRow = sqlRow.concat(config.getGenerateUpdateEvent() ? "" : ", " + config.getUpdatedDateAlias());
        sqlRow = sqlRow.concat(config.getGenerateDeleteEvent() ? "" : ", " + config.getDeletedDateAlias());
        sqlRow = sqlRow.concat(config.getChildrenIdAlias() == null ? "" : ", " + config.getChildrenIdAlias());
        sqlRow = sqlRow.concat(" FROM " + config.getSchemaName());
        sqlRow = sqlRow.concat("." + config.getTableName());
        sqlRow = sqlRow.concat(" WHERE " + config.getTargetKey() + " = " + targetValue);
        log.info("ℹ️ Сформированый запрос: {}", sqlRow);
        return sqlRow;
    }

    private void validateTargetKeyType(Config config) {
        String targetKeyType = config.getTargetKeyType();
        if (targetKeyType == null) {
            throw new ValidationException("targetKeyType не может быть null.");
        }
        if (!targetKeyType.equals("int") && !targetKeyType.equals("text")) {
            log.info("ℹ️ targetKeyType = {}", targetKeyType);
            throw new ValidationException("targetKeyType должен быть text или int.");
        }
        if (targetKeyType.equals("int") && !isInteger(config.getTargetValue())) {
            throw new ValidationException("TargetValue должен соответствовать  типу targetKeyType.");
        }
    }

    public boolean isInteger(String str) {
        return str != null && str.matches("-?\\d+");
    }

    public String createSql(Config config) {
        log.info("ℹ️ Start method createSql");
        String sqlRow = "SELECT ";
        sqlRow = sqlRow.concat(config.getIdAlias() == null || config.getIdAlias()
                .equals("") ? "id" : config.getIdAlias() + " ");
        sqlRow = sqlRow.concat(config.getGetName() ? ", " + config.getNameAlias() : "");
        sqlRow = sqlRow.concat(", " + config.getCreatedDateAlias());
        sqlRow = sqlRow.concat(config.getGetSource() ? ", source" : "");
        sqlRow = sqlRow.concat(config.getGenerateUpdateEvent() ? "" : ", " + config.getUpdatedDateAlias());
        sqlRow = sqlRow.concat(config.getGenerateDeleteEvent() ? "" : ", " + config.getDeletedDateAlias());
        sqlRow = sqlRow.concat(config.getChildrenIdAlias() == null ? "" : ", " + config.getChildrenIdAlias());
        sqlRow = sqlRow.concat(" FROM " + config.getSchemaName());
        sqlRow = sqlRow.concat("." + config.getTableName());
        log.info("ℹ️ Сформированый запрос: {}", sqlRow);
        return sqlRow;
    }
}
