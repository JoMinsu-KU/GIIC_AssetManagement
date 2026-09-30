package org.example.giic_asset.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.AssetStatus;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AssetRequest {
    @NotBlank
    private String assetNo;
    @NotBlank
    private String name;
    private String model;
    private String spec;
    private LocalDate purchaseDate;
    private Long value;
    private String managingDept;
    private String location;
    @NotNull
    private AssetStatus status;
    private Boolean sharedUse;

    // === NEW FIELDS ===
    private String actualUserName;
    private String actualUserDepartment;
    private String actualUserContact;
    // registeredBy will be set from SecurityContext

    public Asset toEntity(User registrar) {
        return Asset.builder()
                .assetNo(assetNo)
                .name(name)
                .model(model)
                .spec(spec)
                .purchaseDate(purchaseDate)
                .value(value)
                .managingDept(managingDept)
                .location(location)
                .status(status)
                .sharedUse(Boolean.TRUE.equals(sharedUse))
                .actualUserName(actualUserName)
                .actualUserDepartment(actualUserDepartment)
                .actualUserContact(actualUserContact)
                .registeredBy(registrar) // From authenticated user
                .build();
    }
}
