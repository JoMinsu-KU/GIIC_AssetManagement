package org.example.giic_asset.repository;

import org.example.giic_asset.entity.AssetUsageChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetUsageChangeHistoryRepository extends JpaRepository<AssetUsageChangeHistory, Long> {
}
