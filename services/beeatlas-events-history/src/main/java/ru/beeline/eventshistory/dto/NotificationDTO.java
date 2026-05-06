/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long entityId;
    private String changeType;
    private String entityType;
}
