package org.example.giic_asset.dto.asset;

import jakarta.validation.constraints.NotNull;

public record AssetCategoryActiveRequest(@NotNull Boolean active) {
}
