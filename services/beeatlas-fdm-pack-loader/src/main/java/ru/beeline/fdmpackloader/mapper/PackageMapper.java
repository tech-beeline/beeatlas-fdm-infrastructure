/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.beeline.fdmpackloader.domain.Package;
import ru.beeline.fdmpackloader.dto.PackageDTO;
import ru.beeline.fdmpackloader.dto.PackageV2DTO;
import ru.beeline.fdmpackloader.repository.PackagePartRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.beeline.fdmpackloader.utils.Constants.ERROR_STATUS;
import static ru.beeline.fdmpackloader.utils.Constants.PROCESS_STATUS;
import static ru.beeline.fdmpackloader.utils.Constants.SUCCESS_STATUS;

@Component
public class PackageMapper {

    @Autowired
    private PackagePartRepository packagePartRepository;

    public PackageDTO convertToDto(Package pack) {
        PackageDTO packageDTO = new PackageDTO();
        packageDTO.setPackageId(pack.getId());
        packageDTO.setOperation(pack.getOperation().getOperationName());
        packageDTO.setStatus(pack.getStatus());
        packageDTO.setAllParts(pack.getCount());
        packageDTO.setCreatedDate(pack.getCreatedDate());
        packageDTO.setSuccessParts(packagePartRepository.getPartsById(pack.getId(), SUCCESS_STATUS));
        packageDTO.setErrorParts(packagePartRepository.getPartsById(pack.getId(), ERROR_STATUS));
        packageDTO.setProcessParts(packagePartRepository.getPartsById(pack.getId(), PROCESS_STATUS));
        return packageDTO;
    }

    public Page<PackageDTO> convertToDto(Page<Package> packages, Pageable pageable, String status) {
        if (packages != null && !packages.isEmpty()) {
            List<PackageDTO> result = packages.stream()
                    .map(pack -> {
                        PackageDTO packageDTO = convertToDto(pack);
                        if (!"All".equals(status)) {
                            packageDTO.setStatus(status);
                        }
                        return packageDTO;
                    })
                    .collect(Collectors.toList());
            return new PageImpl<>(result, pageable, packages.getTotalElements());
        }
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    public Page<PackageV2DTO> convertToV2Dto(Page<Package> packages, Pageable pageable, String status) {
        if (packages != null && !packages.isEmpty()) {
            List<PackageV2DTO> result = packages.stream()
                    .map(pack -> {
                        PackageV2DTO packageDTO = convertToV2Dto(pack);
                        if (!"All".equals(status)) {
                            packageDTO.setStatus(status);
                        }
                        return packageDTO;
                    })
                    .collect(Collectors.toList());
            return new PageImpl<>(result, pageable, packages.getTotalElements());
        }
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    public PackageV2DTO convertToV2Dto(Package pack) {
        PackageV2DTO packageDTO = new PackageV2DTO();
        packageDTO.setPackageId(pack.getId());
        packageDTO.setOperation(pack.getOperation().getOperationName());
        packageDTO.setStatus(pack.getStatus());
        packageDTO.setAllParts(pack.getCount());
        packageDTO.setCreatedDate(pack.getCreatedDate());
        packageDTO.setSource(pack.getSource());
        packageDTO.setSourceId(pack.getSourceId());
        packageDTO.setSuccessParts(packagePartRepository.getPartsById(pack.getId(), SUCCESS_STATUS));
        packageDTO.setErrorParts(packagePartRepository.getPartsById(pack.getId(), ERROR_STATUS));
        packageDTO.setProcessParts(packagePartRepository.getPartsById(pack.getId(), PROCESS_STATUS));
        return packageDTO;
    }
}
