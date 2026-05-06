/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
public class TechradarClient {
    RestTemplate restTemplate;
    private final String techradarServerUrl;

    public TechradarClient(@Value("${integration.techradar-server-url}") String techradarServerUrl,
                           RestTemplate restTemplate) {
        this.techradarServerUrl = techradarServerUrl;
        this.restTemplate = restTemplate;
    }

    public HttpStatus postProductTech(String body) {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.add("SOURCE", "GIT");
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            return restTemplate.exchange(techradarServerUrl + "/api/v1/tech/product-relation",
                    HttpMethod.POST, entity, Object.class).getStatusCode();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
