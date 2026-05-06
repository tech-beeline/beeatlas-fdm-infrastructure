/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory.domain;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "config", schema = "entity_events")
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_generator")
    @SequenceGenerator(name = "user_id_generator", sequenceName = "seq_user_id", allocationSize = 1)
    private Integer id;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "notification_required")
    private Boolean notificationRequired;

    @Column(name = "last_scan_date")
    private LocalDateTime lastScanDate;

    @Column(name = "id_alias")
    private String idAlias;

    @Column(name = "get_name")
    private Boolean getName;

    @Column(name = "get_source")
    private Boolean getSource;

    @Column(name = "created_date_alias")
    private String createdDateAlias;

    @Column(name = "updated_date_alias")
    private String updatedDateAlias;

    @Column(name = "deleted_date_alias")
    private String deletedDateAlias;

    @Column(name = "generate_update_event")
    private Boolean generateUpdateEvent;

    @Column(name = "generate_delete_event")
    private Boolean generateDeleteEvent;

    @Column(name = "name_alias")
    private String nameAlias;

    @Column(name = "clarification_event")
    private String clarificationEvent;

    @Column(name = "children_id_alias")
    private String childrenIdAlias;

    @Column(name = "target_key")
    private String targetKey;

    @Column(name = "target_key_type")
    private String targetKeyType;

    @Column(name = "target_value")
    private String targetValue;
}