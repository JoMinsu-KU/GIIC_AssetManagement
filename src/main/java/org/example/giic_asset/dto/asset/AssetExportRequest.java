package org.example.giic_asset.dto.asset;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AssetExportRequest {
    private List<String> columns;
    private AssetSearchCriteria filters;

    public AssetSearchCriteria getSafeFilters() {
        return filters == null ? AssetSearchCriteria.empty() : filters;
    }
}
