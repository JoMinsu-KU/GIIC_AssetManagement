package org.example.giic_asset.repository;

import org.example.giic_asset.entity.AssetLossRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetLossRequestRepository extends JpaRepository<AssetLossRequest, Long> {
    List<AssetLossRequest> findByRequesterEmployeeNoOrderByCreatedAtDesc(String requesterEmployeeNo);

    List<AssetLossRequest> findAllByOrderByCreatedAtDesc();

    void deleteByAssetId(Long assetId);
}
