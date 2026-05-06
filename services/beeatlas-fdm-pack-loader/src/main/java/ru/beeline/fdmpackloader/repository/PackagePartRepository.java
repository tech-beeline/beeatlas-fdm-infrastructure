/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmpackloader.domain.PackagePart;

@Repository
public interface PackagePartRepository extends JpaRepository<PackagePart, Integer> {
    @Query(value = "SELECT COUNT(*) FROM pack_loader.package_parts " +
            "WHERE id_package = :id AND status_id = (SELECT id FROM pack_loader.status_enum WHERE status = :status)",
            nativeQuery = true)
    Integer getPartsById(@Param("id") Integer id, @Param("status") String status);

    @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END FROM PackagePart pp " +
            "WHERE pp.pack.id = :idPackage AND pp.partNum = :partNum AND pp.statusEnum.status IN ('SUCCESS', 'PROCESS')")
    boolean existsByIdPackageAndPartNumAndStatusIn(@Param("idPackage") Integer idPackage, @Param("partNum") Long partNum);

    PackagePart findByIdPackageAndPartNum(Integer idPackage, Long partNum);

    Page<PackagePart> findAllByIdPackageOrderByPartNum(Integer idPackage, Pageable pageable);

}
