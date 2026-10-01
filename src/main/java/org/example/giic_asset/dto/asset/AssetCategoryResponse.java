package org.example.giic_asset.dto.asset;

import java.util.List;

public record AssetCategoryResponse(Long id, String name, List<Subcategory> subcategories) {
    public record Subcategory(Long id, String name) {
    }
}
