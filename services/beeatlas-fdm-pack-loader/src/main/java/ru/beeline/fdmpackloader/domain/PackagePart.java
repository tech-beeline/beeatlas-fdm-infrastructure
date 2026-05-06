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
@Table(name = "package_parts")
public class PackagePart {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "package_parts_id_generator")
    @SequenceGenerator(name = "package_parts_id_generator", sequenceName = "package_parts_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "id_package")
    private Integer idPackage;

    private String payload;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "part_num")
    private Long partNum;

    @ManyToOne
    @JoinColumn(name = "id_package", referencedColumnName = "id", insertable = false, updatable = false)
    private Package pack;

    @ManyToOne
    @JoinColumn(name = "status_id", referencedColumnName = "id", insertable = false, updatable = false)
    private StatusEnum statusEnum;

}