package org.example.giic_asset.repository;

import org.example.giic_asset.entity.Maintenance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    Page<Maintenance> findByAssetId(Long assetId, Pageable pageable);

    void deleteByAssetId(Long assetId);

    List<Maintenance> findByRequesterEmployeeNoOrderByCreatedAtDesc(String employeeNo);

    @Query("SELECT m FROM Maintenance m ORDER BY m.createdAt DESC")
    List<Maintenance> findAllByOrderByCreatedAtDesc();
}
