package org.example.giic_asset.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.enums.AssetStatus;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSearchCriteria {
    private String keyword;
    private AssetStatus status;
    private String dept;
    private String userId;
    private String actualUserName;
    private String assetNo;
    private String name;
    private String model;
    private String location;
    private LocalDate purchaseDateFrom;
    private LocalDate purchaseDateTo;
    private Long valueMin;
    private Long valueMax;

    public static AssetSearchCriteria empty() {
        return AssetSearchCriteria.builder().build();
    }
}
