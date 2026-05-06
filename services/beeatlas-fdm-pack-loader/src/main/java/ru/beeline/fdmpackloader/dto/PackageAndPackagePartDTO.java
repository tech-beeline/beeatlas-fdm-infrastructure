/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.dto;

import lombok.*;
import org.springframework.data.domain.Page;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageAndPackagePartDTO {
    PackageDTO packageDTO;
    Page<PackagePartDTO> packagePartDTOS;
}
