package org.example.giic_asset.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetCategoryRenameRequest(@NotBlank @Size(max = 100) String name) {
}
