package org.example.giic_asset.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.enums.AssetStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssetResponse {
    private Long id;
    private String assetNo;
    private String name;
    private String model;
    private String spec;
    private LocalDate purchaseDate;
    private Long value;
    private String managingDept;
    private String location;
    private AssetStatus status;
    private LocalDateTime createdAt;

    // === NEW FIELDS ===
    private String actualUserName;
    private String actualUserDepartment;
    private String actualUserContact;
    private String registeredByName;
    private String registeredById;

    // === DISPOSAL FIELDS ===
    private String disposalType;
    private String disposalReason;
    private LocalDateTime disposalRequestDate;
    private String disposalRequesterId;
    private String disposalRejectionReason;

    // === LOSS FIELDS ===
    private LocalDate lossDate;
    private String lossRequesterId;
    private String lossRequesterName;
    private String lossRemarks;

    // === DEPRECATED (for backward compatibility) ===
    @Deprecated
    private String userName; // Map to actualUserName
    @Deprecated
    private String userId; // Will be null for external users

    private List<AssetImageResponse> images;

    public static AssetResponse from(Asset asset, List<AssetImageResponse> images) {
        return AssetResponse.builder()
                .id(asset.getId())
                .assetNo(asset.getAssetNo())
                .name(asset.getName())
                .model(asset.getModel())
                .spec(asset.getSpec())
                .purchaseDate(asset.getPurchaseDate())
                .value(asset.getValue())
                .managingDept(asset.getManagingDept())
                .location(asset.getLocation())
                .status(asset.getStatus())
                .createdAt(asset.getCreatedAt())
                // New fields
                .actualUserName(asset.getActualUserName())
                .actualUserDepartment(asset.getActualUserDepartment())
                .actualUserContact(asset.getActualUserContact())
                .registeredByName(asset.getRegisteredBy() != null ? asset.getRegisteredBy().getName() : null)
                .registeredById(asset.getRegisteredBy() != null ? asset.getRegisteredBy().getEmployeeNo() : null)
                // Disposal fields
                .disposalType(asset.getDisposalType())
                .disposalReason(asset.getDisposalReason())
                .disposalRequestDate(asset.getDisposalRequestDate())
                .disposalRequesterId(asset.getDisposalRequesterId())
                .disposalRejectionReason(asset.getDisposalRejectionReason())
                // Loss fields
                .lossDate(asset.getLossDate())
                .lossRequesterId(asset.getLossRequesterId())
                .lossRequesterName(asset.getLossRequesterName())
                .lossRemarks(asset.getLossRemarks())
                // Deprecated fields for backward compatibility
                .userName(asset.getActualUserName()) // Map to new field
                .userId(null) // No longer relevant
                .images(images)
                .build();
    }
}
