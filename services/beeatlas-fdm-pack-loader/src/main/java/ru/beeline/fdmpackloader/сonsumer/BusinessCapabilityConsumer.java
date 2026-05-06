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
public class BusinessCapabilityConsumer {

    @Autowired
    PackageService packageService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${queue.business-capability.name}")
    public void processBusinessCapabilityQueue(String message) {
        log.info("Received from business_capability_queue : " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = validateCapabilityMessage(message);
            String payloadJson = objectMapper.writeValueAsString(jsonNode.get("payload"));
            packageService.addBusinessCapability(
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

    private JsonNode validateCapabilityMessage(String message) throws MessageValidationException {
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(message);
            if (jsonNode.get("packageId").asInt() < 1
                    && jsonNode.get("count").asInt() < 1
                    && jsonNode.get("part_num").asInt() < 1
            ) {
                throw new Exception();
            }
        } catch (Exception E) {
            throw new MessageValidationException("Message not valid SCHEMA:" + message);
        }
        return jsonNode;
    }
}