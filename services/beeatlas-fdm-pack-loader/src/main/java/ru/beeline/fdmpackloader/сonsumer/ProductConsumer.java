/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.сonsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class ProductConsumer {

    @Autowired
    PackageService packageService;
    private ObjectMapper objectMapper = new ObjectMapper();


    @RabbitListener(queues = "${queue.product.name}")
    public void processPackageQueue(String message) {
        log.info("Received from product_queue : " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = validatePackageMessage(message);
            String payloadJson = objectMapper.writeValueAsString(jsonNode.get("payload"));
            packageService.resendProductPackage(
                    PackagePart.builder()
                            .idPackage(jsonNode.get("packageId").asInt())
                            .partNum(jsonNode.get("part_num").longValue())
                            .payload(payloadJson)
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

            // Проверка наличия обязательных полей
            if (!jsonNode.has("packageId") || !jsonNode.has("count") || !jsonNode.has("part_num") || !jsonNode.has("payload")) {
                throw new Exception("Missing required fields");
            }

            // Проверка типов и значений обязательных полей
            int packageId = jsonNode.get("packageId").asInt();
            int count = jsonNode.get("count").asInt();
            int partNum = jsonNode.get("part_num").asInt();

            if (packageId < 1 || count < 1 || partNum < 1 || partNum > count) {
                throw new Exception("Invalid field values");
            }

            JsonNode payload = jsonNode.get("payload");
            if (!payload.has("cmdbCode")) {
                throw new Exception("Missing required fields in payload");
            }
            if (!payload.has("name") || payload.get("name").isNull()) {
                ((ObjectNode) payload).put("name", "");
            }
        } catch (Exception e) {
            throw new MessageValidationException("Message not valid SCHEMA: " + message);
        }
        return jsonNode;
    }
}