/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.eventshistory.service.EventService;

@RestController
@EnableScheduling
public class ApplicationController {
    @Autowired
    EventService eventService;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.name}")
    private String appName;


    @GetMapping("/")
    public String getData() {
        return "Welcome " + appName + " " + appVersion;
    }

    @GetMapping("/test-send-events")
    public String create() {
        eventService.createAndSendEvents();
        return "Done";
    }
}