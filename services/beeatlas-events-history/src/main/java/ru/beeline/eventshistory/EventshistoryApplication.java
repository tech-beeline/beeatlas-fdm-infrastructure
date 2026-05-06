/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@SpringBootApplication
public class EventshistoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventshistoryApplication.class, args);
    }
}
