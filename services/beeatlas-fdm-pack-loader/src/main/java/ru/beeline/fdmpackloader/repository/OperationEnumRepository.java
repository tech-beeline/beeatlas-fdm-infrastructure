/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmpackloader.domain.OperationEnum;

import java.util.Optional;

@Repository
public interface OperationEnumRepository extends JpaRepository<OperationEnum, Long> {
    Optional<OperationEnum> findByOperationName(String operationName);
}
