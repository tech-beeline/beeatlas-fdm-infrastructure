/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.сonsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmpackloader.domain.PackagePart;
import ru.beeline.fdmpackloader.exception.MessageValidationException;
import ru.beeline.fdmpackloader.service.PackageService;
import ru.beeline.fdmpackloader.utils.StatusEnumUtils;

import static ru.beeline.fdmpackloader.utils.Constants.PROCESS_STATUS;


@Slf4j
@Component
@EnableRabbit
public class TechProductRelationConsumer {

    @Autowired
    PackageService packageService;
    private ObjectMapper objectMapper = new ObjectMapper();


    @RabbitListener(queues = "${queue.tech-product-relation.name}")
    public void processTechProductRelationQueue(String message) {
        log.info("Received from package_queue : " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = validatePackageMessage(message);
            String payloadJson = objectMapper.writeValueAsString(jsonNode.get("payload"));
            packageService.addTechProductRelation(
                    PackagePart.builder()
                            .idPackage(jsonNode.get("packageId").asInt())
                            .payload(payloadJson)
                            .partNum(jsonNode.get("part_num").asLong())
                            .statusId(StatusEnumUtils.getIdByStatus(PROCESS_STATUS))
                            .build(), jsonNode.get("count").asInt());

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    private JsonNode validatePackageMessage(String message) throws MessageValidationException {
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(message);

            if (jsonNode.get("packageId").asInt() < 1
                    || jsonNode.get("count").asInt() < 1
                    || jsonNode.get("part_num").asInt() > jsonNode.get("count").asInt()
                    || jsonNode.get("payload").get("cmdb_code") == null
                    || jsonNode.get("payload").get("proj_lang") == null
            ) {
                throw new Exception();
            }
        } catch (Exception E) {
            throw new MessageValidationException("Message not valid SCHEMA:" + message);
        }
        return jsonNode;
    }

}