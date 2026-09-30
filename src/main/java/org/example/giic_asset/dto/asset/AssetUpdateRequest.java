package org.example.giic_asset.dto.asset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.enums.AssetStatus;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetUpdateRequest {
    private Long assetNo;
    private String name;
    private String model;
    private String spec;
    private Long value;
    private java.time.LocalDate purchaseDate;
    private String managingDept;
    private String location;
    private AssetStatus status;

    // === NEW FIELDS ===
    private String actualUserName;
    private String actualUserDepartment;
    private String actualUserContact;
}
