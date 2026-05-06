/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.eventshistory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.eventshistory.domain.Config;

import java.time.LocalDateTime;
import java.util.List;

public interface ConfigRepository extends JpaRepository<Config, Integer> {

    @Modifying
    @Query("UPDATE Config c SET c.lastScanDate = :now WHERE c.lastScanDate IS NULL")
    int updateLastScanDateForNull(@Param("now") LocalDateTime now);
}
