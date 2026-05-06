/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmpackloader.dto.product.ProductPutDto;
import ru.beeline.fdmpackloader.client.CapabilityClient;
import ru.beeline.fdmpackloader.client.ProductClient;
import ru.beeline.fdmpackloader.client.TechradarClient;
import ru.beeline.fdmpackloader.domain.OperationEnum;
import ru.beeline.fdmpackloader.domain.Package;
import ru.beeline.fdmpackloader.domain.PackagePart;
import ru.beeline.fdmpackloader.dto.PackageAndPackagePartDTO;
import ru.beeline.fdmpackloader.dto.PackageDTO;
import ru.beeline.fdmpackloader.dto.PackageRegistrationRequestDTO;
import ru.beeline.fdmpackloader.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmpackloader.dto.PackageV2DTO;
import ru.beeline.fdmpackloader.exception.ForbiddenException;
import ru.beeline.fdmpackloader.exception.MessageValidationException;
import ru.beeline.fdmpackloader.exception.NotFoundException;
import ru.beeline.fdmpackloader.mapper.PackageMapper;
import ru.beeline.fdmpackloader.mapper.PackagePartMapper;
import ru.beeline.fdmpackloader.repository.OperationEnumRepository;
import ru.beeline.fdmpackloader.repository.PackagePartRepository;
import ru.beeline.fdmpackloader.repository.PackageRepository;
import ru.beeline.fdmpackloader.utils.StatusEnumUtils;

import javax.persistence.EntityNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static ru.beeline.fdmpackloader.controller.RequestContext.getUserRoles;
import static ru.beeline.fdmpackloader.utils.Constants.ERROR_STATUS;
import static ru.beeline.fdmpackloader.utils.Constants.PACKAGE_PARSING;
import static ru.beeline.fdmpackloader.utils.Constants.PROCESS_STATUS;
import static ru.beeline.fdmpackloader.utils.Constants.SUCCESS_STATUS;
import static ru.beeline.fdmpackloader.utils.Constants.VALIDATE_ERROR;
import static ru.beeline.fdmpackloader.utils.Constants.WARNING_STATUS;

@Slf4j
@Service
@Transactional
public class PackageService {
    private static final String IN_QUEUE = "IN QUEUE";
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private OperationEnumRepository operationEnumRepository;
    @Autowired
    private PackageMapper packageMapper;
    @Autowired
    private PackagePartMapper packagePartMapper;
    @Autowired
    private PackagePartRepository packagePartRepository;
    @Autowired
    private CapabilityClient capabilityClient;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private TechradarClient techradarClient;
    @Autowired
    private RabbitService rabbitService;

    public PackageRegistrationResponseDTO registration(PackageRegistrationRequestDTO packageRegistrationRequestDTO,
                                                       String source, String status, Integer sourceId) {
        log.info("Start registration method");
        if (packageRegistrationRequestDTO.getCount() < 1 || packageRegistrationRequestDTO.getOperation() == null) {
            throw new MessageValidationException("Not valid request");
        }
        log.info("Operation: " + packageRegistrationRequestDTO.getOperation());
        Optional<OperationEnum> operationEnum = operationEnumRepository.findByOperationName(packageRegistrationRequestDTO.getOperation());
        if (operationEnum.isEmpty()) {
            throw new MessageValidationException("Not valid request");
        }


        Package result = packageRepository.save(Package.builder()
                .operationId(operationEnum.get().getId())
                .count(packageRegistrationRequestDTO.getCount())
                .status(status != null && !status.isEmpty() ? status : IN_QUEUE)
                .createdDate(new Date())
                .source(source != null ? source : "Sparx")
                .sourceId(sourceId)
                .build());
        log.info("the package is saved");
        return PackageRegistrationResponseDTO.builder().packageId(result.getId()).build();
    }

    public Page<PackageDTO> getPackageList(String status, int limit, int offset) {
        if (getUserRoles().contains("ADMIN")) {
            throw new ForbiddenException("FORBIDDEN");
        }
        switch (status) {
            case SUCCESS_STATUS:
                return packageMapper.convertToDto(packageRepository.findPackagesWithStatusAndSuccessPartsEquals(PageRequest.of(offset, limit)),
                        PageRequest.of(offset, limit), "SUCCESS");
            case ERROR_STATUS:
                return packageMapper.convertToDto(packageRepository.findByStatus(status, PageRequest.of(offset, limit)),
                        PageRequest.of(offset, limit), "ERROR");
            case WARNING_STATUS:
                return packageMapper.convertToDto(packageRepository.findPackagesWithStatusAndErrorPartsNotZero(PageRequest.of(offset, limit)),
                        PageRequest.of(offset, limit), "WARNING");
            case PROCESS_STATUS:
                return packageMapper.convertToDto(packageRepository.findByStatusInAndLimitAndOffset(List.of("IN QUEUE",
                        "PACKAGE PARSING", "PACKAGE PARTS PROCESSING"), PageRequest.of(offset, limit)), PageRequest.of(offset, limit), "PROCESS");
            default:
                Page<PackageDTO> packageDTOs = packageMapper.convertToDto(packageRepository.findAll(PageRequest.of(offset,
                        limit)), PageRequest.of(offset, limit), "All");
                for (PackageDTO packageDTO : packageDTOs) {
                    packageDTO.setStatus(getStatus(packageDTO));
                }
                return new PageImpl<>(packageDTOs.getContent(), PageRequest.of(offset, limit), packageDTOs.getTotalElements());
        }
    }

    public Page<PackageV2DTO> getPackageV2List(String status, Integer limit, Integer offset, String source) {
        if (getUserRoles().contains("ADMIN")) {
            throw new ForbiddenException("FORBIDDEN");
        }
        if (limit == null || limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        switch (status) {
            case SUCCESS_STATUS:
                return packageMapper.convertToV2Dto(packageRepository.findPackagesWithStatusAndSuccessPartsEqualsV2(PageRequest.of(offset, limit),
                        source), PageRequest.of(offset, limit), "SUCCESS");
            case ERROR_STATUS:
                return packageMapper.convertToV2Dto(packageRepository.findByStatusAndOptionalSourceV2(status, source,
                                PageRequest.of(offset, limit)),
                        PageRequest.of(offset, limit), "ERROR");
            case WARNING_STATUS:
                return packageMapper.convertToV2Dto(packageRepository.findPackagesWithStatusAndErrorPartsNotZeroV2(source,
                        PageRequest.of(offset, limit)), PageRequest.of(offset, limit), "WARNING");
            case PROCESS_STATUS:
                return packageMapper.convertToV2Dto(packageRepository.findByStatusInAndLimitAndOffsetV2(List.of("IN QUEUE",
                                "PACKAGE PARSING", "PACKAGE PARTS PROCESSING"), source, PageRequest.of(offset, limit)),
                        PageRequest.of(offset, limit), "PROCESS");
            case VALIDATE_ERROR:
                return packageMapper.convertToV2Dto(packageRepository.findByStatusAndOptionalSourceV2(status, source,
                                PageRequest.of(offset, limit)),
                        PageRequest.of(offset, limit), "VALIDATE ERROR");
            default:
                Page<PackageV2DTO> packageDTOs = packageMapper.convertToV2Dto(packageRepository.findAllWithOptionalSourceFilter(source,
                        PageRequest.of(offset, limit)), PageRequest.of(offset, limit), "All");
                for (PackageV2DTO packageDTO : packageDTOs) {
                    packageDTO.setStatus(getStatus(packageDTO));
                }
                return new PageImpl<>(packageDTOs.getContent(), PageRequest.of(offset, limit), packageDTOs.getTotalElements());
        }
    }

    public void addTechCapability(PackagePart packagePart, int count) throws Exception {
        String source = existPackage(packagePart.getIdPackage()).getSource();
        existsByPackageIdAndPartNumAndStatusIn(packagePart.getIdPackage(), packagePart.getPartNum());
        PackagePart part = packagePartRepository.findByIdPackageAndPartNum(packagePart.getIdPackage(), packagePart.getPartNum());
        if (part != null) {
            part.setStatusId(StatusEnumUtils.getIdByStatus(PROCESS_STATUS));
            packagePartRepository.save(part);
        } else {
            packagePartRepository.save(packagePart);
        }
        try {
            HttpStatus httpStatus = capabilityClient.putTechCapability(packagePart.getPayload(), source);
            if (httpStatus != null && httpStatus.is2xxSuccessful()) {
                packagePart.setStatusId(StatusEnumUtils.getIdByStatus(SUCCESS_STATUS));
                packagePartRepository.save(packagePart);
            } else {
                packagePart.setStatusId(StatusEnumUtils.getIdByStatus(ERROR_STATUS));
                packagePartRepository.save(packagePart);
            }
            if (packagePart.getPartNum() == count) {
                packageRepository.setDoneById(packagePart.getIdPackage());
            }
        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    public void addBusinessCapability(PackagePart packagePart, int count) throws Exception {
        String source = existPackage(packagePart.getIdPackage()).getSource();
        existsByPackageIdAndPartNumAndStatusIn(packagePart.getIdPackage(), packagePart.getPartNum());
        PackagePart part = packagePartRepository.findByIdPackageAndPartNum(packagePart.getIdPackage(), packagePart.getPartNum());
        if (part != null) {
            throw new Exception("A record with the specified id_package and part_num already exists in the status of 'SUCCESS' or 'PROCESS'");
        } else {
            packagePartRepository.save(packagePart);
        }
        try {
            HttpStatus httpStatus = capabilityClient.putBusinessCapability(packagePart.getPayload(), source);
            if (httpStatus != null && httpStatus.is2xxSuccessful()) {
                packagePart.setStatusId(StatusEnumUtils.getIdByStatus(SUCCESS_STATUS));
                packagePartRepository.save(packagePart);
            } else {
                packagePart.setStatusId(StatusEnumUtils.getIdByStatus(ERROR_STATUS));
                packagePartRepository.save(packagePart);
            }

            if (packagePart.getPartNum() == count) {
                packageRepository.setDoneById(packagePart.getIdPackage());
            }
        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    public void addTechProductRelation(PackagePart packagePart, int count) throws Exception {
        existPackage(packagePart.getIdPackage());
        existsByPackageIdAndPartNumAndStatusIn(packagePart.getIdPackage(), packagePart.getPartNum());
        PackagePart part = packagePartRepository.findByIdPackageAndPartNum(packagePart.getIdPackage(), packagePart.getPartNum());
        if (part != null) {
            part.setStatusId(StatusEnumUtils.getIdByStatus(PROCESS_STATUS));
            packagePartRepository.save(part);
        } else {
            part = packagePart;
        }
        try {
            HttpStatus httpStatus = techradarClient.postProductTech(part.getPayload());
            if (httpStatus != null && httpStatus.is2xxSuccessful()) {
                part.setStatusId(StatusEnumUtils.getIdByStatus(SUCCESS_STATUS));
                packagePartRepository.save(part);
            } else {
                part.setStatusId(StatusEnumUtils.getIdByStatus(ERROR_STATUS));
                packagePartRepository.save(part);
                throw new Exception("A record with the specified id_package and part_num already exists in the status of 'SUCCESS' or 'PROCESS'");
            }

            if (part.getPartNum() == count) {
                packageRepository.setDoneById(part.getIdPackage());
            }
        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    public void existsByPackageIdAndPartNumAndStatusIn(Integer packageId, Long partNum) throws Exception {
        if (packagePartRepository.existsByIdPackageAndPartNumAndStatusIn(packageId, partNum)) {
            throw new Exception("A record with the specified id_package and part_num already exists in the status of 'SUCCESS' or 'PROCESS'");
        }
    }

    public Package existPackage(Integer packageId) {
        Optional<Package> pack = packageRepository.findById(packageId);
        if (pack.isEmpty()) {
            throw new EntityNotFoundException("Package with the specified ID does not exist");
        }
        return pack.get();
    }

    public void resendPackage(PackagePart part) throws JsonProcessingException {
        Package pack = existPackage(part.getIdPackage());
        pack.setStatus(PACKAGE_PARSING);
        packageRepository.save(pack);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode payloadArray = objectMapper.readTree(part.getPayload());

        for (int i = 0; i < payloadArray.size(); i++) {
            ObjectNode messagePayload = objectMapper.createObjectNode();
            messagePayload.put("packageId", part.getIdPackage());
            messagePayload.put("count", payloadArray.size());
            messagePayload.put("part_num", i + 1);
            messagePayload.set("payload", payloadArray.get(i));

            String message = objectMapper.writeValueAsString(messagePayload);

            rabbitService.sendMessage(pack.getOperation().getQueueName(), message);
        }

        pack.setStatus(PACKAGE_PARSING);
        packageRepository.save(pack);
    }

    public void resendProductPackage(PackagePart packagePart, int count) throws Exception {
        existPackage(packagePart.getIdPackage());
        PackagePart part = packagePartRepository.findByIdPackageAndPartNum(packagePart.getIdPackage(), packagePart.getPartNum());
        if (part != null) {
            if (part.getStatusId() < 3) {
                throw new Exception("A record with the specified id_package and part_num already exists in the status of 'SUCCESS' or 'PROCESS'");
            }
            part.setStatusId(StatusEnumUtils.getIdByStatus(PROCESS_STATUS));
            packagePartRepository.save(part);
        }
        packagePartRepository.save(packagePart);
        try {
            HttpStatus httpStatus = sendProductPutRequest(packagePart);

            if (httpStatus != null && httpStatus.is2xxSuccessful()) {
                packagePart.setStatusId(StatusEnumUtils.getIdByStatus(SUCCESS_STATUS));
                packagePartRepository.save(packagePart);
            } else {
                packagePart.setStatusId(StatusEnumUtils.getIdByStatus(ERROR_STATUS));
                packagePartRepository.save(packagePart);
            }

            if (packagePart.getPartNum() == count) {
                packageRepository.setDoneById(packagePart.getIdPackage());
            }
        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    private HttpStatus sendProductPutRequest(PackagePart packagePart) throws Exception {
        JsonNode payload = objectMapper.readTree(packagePart.getPayload());
        String name = payload.has("name") && !payload.get("name").isNull() ? payload.get("name").asText() : null;
        String description = payload.has("description") &&
                !payload.get("description").isNull() ? payload.get("description").asText() : null;
        String vcsRepositoryUrl = payload.has("vcsRepository") &&
                payload.get("vcsRepository").has("url") &&
                !payload.get("vcsRepository").get("url").isNull() ? payload.get("vcsRepository").get("url").asText() : null;
        ProductPutDto productPutDto = new ProductPutDto();
        if (name != null) {
            productPutDto.setName(name);
        }
        if (description != null) {
            productPutDto.setDescription(description);
        }
        if (vcsRepositoryUrl != null) {
            productPutDto.setGitUrl(vcsRepositoryUrl);
        }
        return productClient.putProduct(productPutDto,
                objectMapper.readTree(packagePart.getPayload()).get("cmdbCode").asText().toLowerCase());
    }

    public PackageAndPackagePartDTO getPackageParts(Integer id, Integer limit, Integer offset) {
        PageRequest pageRequest = PageRequest.of(offset, limit);
        Page<PackagePart> packageParts = packagePartRepository.findAllByIdPackageOrderByPartNum(id, PageRequest.of(offset, limit));
        if (packageParts.isEmpty()) throw new NotFoundException("Not found");
        PackageDTO packageDTO = packageMapper.convertToDto(packageRepository.findById(id).orElseThrow(() -> new NotFoundException("Not found")));
        packageDTO.setStatus(getStatus(packageDTO));
        PackageAndPackagePartDTO result = new PackageAndPackagePartDTO();
        result.setPackagePartDTOS(packagePartMapper.convertToDto(packageParts, pageRequest));
        result.setPackageDTO(packageDTO);
        return result;
    }

    private String getStatus(PackageDTO packageDTO) {
        String status = packageDTO.getStatus();
        switch (status) {
            case "DONE":
                if (packageDTO.getSuccessParts() == packageDTO.getAllParts()) {
                    return "SUCCESS";
                } else if (packageDTO.getErrorParts() != 0) {
                    return "WARNING";
                }
                break;
            case "ERROR":
                return "ERROR";
            case "VALIDATE ERROR":
                return "VALIDATE ERROR";
            case "IN QUEUE":
            case "PACKAGE PARSING":
            case "PACKAGE PARTS PROCESSING":
                return "PROCESS";
        }
        return status;
    }

}