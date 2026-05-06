/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmpackloader.dto.product.ProductPutDto;

@Slf4j
@Service
public class ProductClient {

    RestTemplate restTemplate;
    private final String productServerUrl;

    public ProductClient(@Value("${integration.products-server-url}") String productServerUrl, RestTemplate restTemplate) {
        this.productServerUrl = productServerUrl;
        this.restTemplate = restTemplate;
    }

    public HttpStatus putProduct(ProductPutDto productPutDto, String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");

            HttpEntity<ProductPutDto> entity = new HttpEntity(productPutDto, headers);
            return restTemplate.exchange(productServerUrl + "/api/v1/product/" + code,
                    HttpMethod.PUT, entity, Object.class).getStatusCode();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
