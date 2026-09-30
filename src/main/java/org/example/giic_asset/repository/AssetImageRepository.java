package org.example.giic_asset.repository;

import org.example.giic_asset.entity.AssetImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetImageRepository extends JpaRepository<AssetImage, Long> {
    List<AssetImage> findByAssetId(Long assetId);

    @Query("select image from AssetImage image "
            + "where image.asset.id = :assetId "
            + "order by image.isPrimary desc, image.id asc")
    List<AssetImage> findOrderedByAssetId(@Param("assetId") Long assetId);

    Optional<AssetImage> findByIdAndAssetId(Long imageId, Long assetId);

    void deleteByAssetId(Long assetId);
}
