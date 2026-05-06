/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmpackloader.domain.StatusEnum;

@Repository
public interface StatusRepository extends JpaRepository<StatusEnum, Long> {

}
