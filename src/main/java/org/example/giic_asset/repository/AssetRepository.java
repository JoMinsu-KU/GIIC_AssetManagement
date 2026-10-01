package org.example.giic_asset.repository;

import org.example.giic_asset.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
    Optional<Asset> findByAssetNo(String assetNo);

    boolean existsByAssetNo(String assetNo);

    List<Asset> findAllByOrderByCreatedAtDesc();

    @Query(value = "SELECT DISTINCT TRIM(location) FROM assets "
            + "WHERE location IS NOT NULL AND TRIM(location) <> '' ORDER BY TRIM(location)", nativeQuery = true)
    List<String> findDistinctLocations();

    @Override
    @EntityGraph(attributePaths = {"registeredBy", "category", "category.parent"})
    Page<Asset> findAll(Specification<Asset> specification, Pageable pageable);
}
