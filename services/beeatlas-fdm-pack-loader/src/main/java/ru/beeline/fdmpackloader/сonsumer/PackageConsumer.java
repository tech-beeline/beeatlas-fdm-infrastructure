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
public class PackageConsumer {

    @Autowired
    PackageService packageService;
    private ObjectMapper objectMapper = new ObjectMapper();


    @RabbitListener(queues = "${queue.package.name}")
    public void processPackageQueue(String message) {
        log.info("Received from package_queue : " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = validatePackageMessage(message);
            String payloadJson = objectMapper.writeValueAsString(jsonNode.get("payload"));
            packageService.resendPackage(
                    PackagePart.builder()
                            .idPackage(jsonNode.get("packageId").asInt())
                            .payload(payloadJson)
                            .statusId(StatusEnumUtils.getIdByStatus(PROCESS_STATUS))
                            .build());

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    private JsonNode validatePackageMessage(String message) throws MessageValidationException {
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(message);
            jsonNode.get("payload");
            if (jsonNode.get("packageId").asInt() < 1
            ) {
                throw new Exception();
            }
        } catch (Exception E) {
            throw new MessageValidationException("Message not valid SCHEMA:" + message);
        }
        return jsonNode;
    }

}