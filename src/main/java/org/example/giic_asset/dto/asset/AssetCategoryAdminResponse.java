package org.example.giic_asset.dto.asset;

import org.example.giic_asset.entity.AssetCategory;

public record AssetCategoryAdminResponse(Long id, String code, String name, Long parentId,
                                         boolean active, int sortOrder) {
    public static AssetCategoryAdminResponse from(AssetCategory category) {
        return new AssetCategoryAdminResponse(category.getId(), category.getCode(), category.getName(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.isActive(), category.getSortOrder());
    }
}
