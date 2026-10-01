package org.example.giic_asset.repository;

import org.example.giic_asset.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    boolean existsByCode(String code);
}
