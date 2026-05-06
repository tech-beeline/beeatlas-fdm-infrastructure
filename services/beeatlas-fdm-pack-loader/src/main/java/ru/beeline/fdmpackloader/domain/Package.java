/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.domain;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "packages")
public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "packages_id_generator")
    @SequenceGenerator(name = "packages_id_generator", sequenceName = "packages_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "operation_id")
    private Integer operationId;

    private Integer count;

    private String status;

    private String payload;

    @Column(name = "created_date")
    private Date createdDate;

    private String source;

    @Column(name = "source_id")
    private Integer sourceId;

    @ManyToOne
    @JoinColumn(name = "operation_id", referencedColumnName = "id", insertable = false, updatable = false)
    private OperationEnum operation;
}