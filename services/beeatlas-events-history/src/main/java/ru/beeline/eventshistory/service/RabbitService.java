/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.eventshistory.client.AuthSSOClient;

@Slf4j
@Component
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CachingConnectionFactory connectionFactory;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private AuthSSOClient authSSOClient;

    public void sendMessage(ObjectNode event, String queue, String entityType, String clarificationEvent) {
        log.info("Start sendMessage method");
        try {
            if (!isConnected()) {
                log.info("refresh Connection");
                refreshConnection();
            }
            JsonNode entityId = event.get("id");
            event.remove("id");
            event.set("entityId", entityId);
            String addOn = clarificationEvent != null ? clarificationEvent : "";
            event.put("entityType", entityType);
            event.put("changeType", event.get("changeType").asText() + addOn);
            event.remove("lastUpdateDate");
            String jsonString = objectMapper.writeValueAsString(event);
            log.info("message to the notification: " + jsonString);
            rabbitTemplate.convertAndSend(queue, jsonString, messagePostProcessor -> {
                messagePostProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return messagePostProcessor;
            });
            log.info("The message has been sent");
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());

        } catch (Exception e) {
            log.error("Error sending message: ", e);
        }
    }

    public void sendEvent(ObjectNode event, String entityType) {
        log.info("method sendEvent start ");
        try {
            if (!isConnected()) {
                refreshConnection();
            }
            if (event.has("entityId")) {
                JsonNode entityId = event.get("entityId");
                event.remove("entityId");
                event.set("id", entityId);
            }
            event.remove("entityType");
            event.remove("lastUpdateDate");
            event.remove("childrenId");
            String jsonString = objectMapper.writeValueAsString(event);
            log.info("send Event message: " + jsonString);
            rabbitTemplate.convertAndSend(entityType, "", jsonString, messagePostProcessor -> {
                messagePostProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return messagePostProcessor;
            });
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON: {}", e.getMessage());

        } catch (Exception e) {
            log.error("Error sending message: ", e);
        }
    }

    private boolean isConnected() {
        try {
            connectionFactory.createConnection().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void refreshConnection() {
        try {
            connectionFactory.resetConnection();
            connectionFactory.setPassword(authSSOClient.getToken());
        } catch (Exception e) {
            log.error("Error refreshing connection: ", e);
        }
    }
}