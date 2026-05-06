/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmpackloader.dto.capability.PutBusinessCapabilityDTO;
import ru.beeline.fdmpackloader.dto.capability.PutTechCapabilityDTO;

@Slf4j
@Service
public class CapabilityClient {

    RestTemplate restTemplate;
    private final String capabilityServerUrl;

    public CapabilityClient(@Value("${integration.capability-server-url}") String capabilityServerUrl, RestTemplate restTemplate) {
        this.capabilityServerUrl = capabilityServerUrl;
        this.restTemplate = restTemplate;
    }

    public HttpStatus putTechCapability(String body, String source) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", source);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(capabilityServerUrl + "/api/v1/tech-capabilities",
                    HttpMethod.PUT, entity, PutTechCapabilityDTO.class).getStatusCode();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public HttpStatus putBusinessCapability(String body, String source) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", source);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(capabilityServerUrl + "/api/v1/business-capability",
                    HttpMethod.PUT, entity, PutBusinessCapabilityDTO.class).getStatusCode();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
