/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.domain;

import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "operation_enum")
public class OperationEnum {
    @Id
    @GeneratedValue
    private Integer id;

    @Column(name = "operation_name")
    private String operationName;

    @Column(name = "queue_name")
    private String queueName;

}