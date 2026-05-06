/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.beeline.fdmpackloader.domain.PackagePart;
import ru.beeline.fdmpackloader.dto.PackagePartDTO;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PackagePartMapper {

    public PackagePartDTO convertToDto(PackagePart packagePart) {
        PackagePartDTO packagePartDTO = new PackagePartDTO();
        packagePartDTO.setPartId(packagePart.getId());
        packagePartDTO.setPartNum(packagePart.getPartNum());
        packagePartDTO.setStatus(packagePart.getStatusEnum().getStatus());
        packagePartDTO.setPayload(packagePart.getPayload());
        return packagePartDTO;
    }

    public Page<PackagePartDTO> convertToDto(Page<PackagePart> packageParts, Pageable pageable) {
        if (packageParts != null && !packageParts.isEmpty()) {
            List<PackagePartDTO> result = packageParts.stream().map(this::convertToDto).collect(Collectors.toList());
            return new PageImpl<>(result, pageable, packageParts.getTotalElements());
        }
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
}
