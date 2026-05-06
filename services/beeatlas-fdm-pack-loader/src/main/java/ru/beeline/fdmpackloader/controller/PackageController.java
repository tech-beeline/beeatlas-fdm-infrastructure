/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmpackloader.aspect.AdminAccessControl;
import ru.beeline.fdmpackloader.dto.PackageAndPackagePartDTO;
import ru.beeline.fdmpackloader.dto.PackageDTO;
import ru.beeline.fdmpackloader.dto.PackageRegistrationRequestDTO;
import ru.beeline.fdmpackloader.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmpackloader.service.PackageService;

@RestController
@RequestMapping("/api/v1")
public class PackageController {

    @Autowired
    private PackageService packageService;

    @PostMapping("/package")
    @ApiOperation(value = "Регистрация пакета")
    public ResponseEntity<PackageRegistrationResponseDTO> registration(
            @RequestBody PackageRegistrationRequestDTO packageRegistrationRequestDTO,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "source_id", required = false) Integer sourceId) {
        return ResponseEntity.ok(packageService.registration(packageRegistrationRequestDTO, source, status, sourceId));
    }

    @GetMapping("/packages-list")
    @ApiOperation(value = "Получение списка пакетов")
    public Page<PackageDTO> getKidsById(@RequestParam(value = "status", required = false, defaultValue = "none") String status,
                                        @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit,
                                        @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
        return packageService.getPackageList(status, limit, offset);
    }

    @AdminAccessControl
    @GetMapping("/package/{id}")
    @ApiOperation(value = "Информация о пакете")
    public PackageAndPackagePartDTO getPackageParts(@PathVariable Integer id,
                                                    @RequestParam(value = "limit", required = false, defaultValue = "" + Integer.MAX_VALUE) Integer limit,
                                                    @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
        return packageService.getPackageParts(id, limit, offset);
    }

}
