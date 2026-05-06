/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmpackloader.domain.Package;

import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<Package, Integer> {
    @Query("SELECT DISTINCT p FROM Package p JOIN PackagePart pp ON p.id = pp.pack.id WHERE p.status = 'DONE' AND " +
            "(SELECT COUNT(ppp) FROM PackagePart ppp WHERE ppp.pack.id = p.id AND ppp.statusEnum.status = 'SUCCESS') = p.count")
    Page<Package> findPackagesWithStatusAndSuccessPartsEquals(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Package p JOIN PackagePart pp ON p.id = pp.pack.id " +
            "WHERE p.status = 'DONE' AND " +
            "(SELECT COUNT(ppp) FROM PackagePart ppp WHERE ppp.pack.id = p.id AND ppp.statusEnum.status = 'SUCCESS') = p.count " +
            "AND (:source IS NULL OR p.source = :source)")
    Page<Package> findPackagesWithStatusAndSuccessPartsEqualsV2(Pageable pageable, @Param("source") String source);

    Page<Package> findByStatus(String status, Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.status = :status AND (:source IS NULL OR p.source = :source)")
    Page<Package> findByStatusAndOptionalSourceV2(@Param("status") String status, @Param("source") String source, Pageable pageable);

    @Query("SELECT p FROM Package p WHERE :source IS NULL OR p.source = :source")
    Page<Package> findAllWithOptionalSourceFilter(@Param("source") String source, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Package p JOIN PackagePart pp ON p.id = pp.pack.id WHERE p.status = 'DONE' AND " +
            "(SELECT COUNT(ppp) FROM PackagePart ppp WHERE ppp.pack.id = p.id AND ppp.statusEnum.status = 'ERROR') != 0")
    Page<Package> findPackagesWithStatusAndErrorPartsNotZero(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Package p JOIN PackagePart pp ON p.id = pp.pack.id WHERE p.status = 'DONE' AND " +
            "(SELECT COUNT(ppp) FROM PackagePart ppp WHERE ppp.pack.id = p.id AND ppp.statusEnum.status = 'ERROR') != 0 AND " +
            "(:source IS NULL OR p.source = :source)")
    Page<Package> findPackagesWithStatusAndErrorPartsNotZeroV2(@Param("source") String source, Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.status IN :statusList")
    Page<Package> findByStatusInAndLimitAndOffset(@Param("statusList") List<String> statusList, Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.status IN :statusList AND (:source IS NULL OR p.source = :source)")
    Page<Package> findByStatusInAndLimitAndOffsetV2(
            @Param("statusList") List<String> statusList,
            @Param("source") String source,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Package p SET p.status = 'DONE' WHERE p.id = :idPackage")
    void setDoneById(@Param("idPackage") Integer idPackage);
}
