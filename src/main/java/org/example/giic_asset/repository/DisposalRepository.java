package org.example.giic_asset.repository;

import org.example.giic_asset.entity.Disposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisposalRepository extends JpaRepository<Disposal, Long> {
    List<Disposal> findByRequesterEmployeeNoOrderByCreatedAtDesc(String requesterEmployeeNo);

    List<Disposal> findAllByOrderByCreatedAtDesc();

    void deleteByAssetId(Long assetId);
}
