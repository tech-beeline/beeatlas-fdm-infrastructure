/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmpackloader.dto.PackageV2DTO;
import ru.beeline.fdmpackloader.service.PackageService;

@RestController
@RequestMapping("/api/v2")
public class PackageV2Controller {

    @Autowired
    private PackageService packageService;

    @GetMapping("/packages-list")
    @ApiOperation(value = "Получение списка пакетов v2")
    public Page<PackageV2DTO> getKidsById(@RequestParam(value = "status", required = false, defaultValue = "none") String status,
                                          @RequestParam(value = "source", required = false) String source,
                                          @RequestParam(value = "limit", required = false) Integer limit,
                                          @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
        return packageService.getPackageV2List(status, limit, offset, source);
    }
}
