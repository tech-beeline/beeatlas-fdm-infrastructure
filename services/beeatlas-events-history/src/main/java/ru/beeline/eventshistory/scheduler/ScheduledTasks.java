/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.beeline.eventshistory.service.EventService;

import java.time.LocalDateTime;

@Component
public class ScheduledTasks {
    @Autowired
    EventService eventService;

    @Scheduled(fixedRateString = "${scheduler.fixedRate}")
    public void createEvents() {
        eventService.createAndSendEvents();
    }
}