package org.example.giic_asset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "asset_usage_change_history")
public class AssetUsageChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long assetId;

    @Column(nullable = false)
    private String assetNo;

    @Column(length = 100)
    private String previousUserName;

    @Column(length = 100)
    private String previousUserDepartment;

    @Column(length = 100)
    private String previousUserContact;

    // The original survey import uses this column for its file name.
    @Column(nullable = false, length = 100)
    private String sourceFile;

    @Column(nullable = false)
    private LocalDateTime capturedAt;

    public static AssetUsageChangeHistory forSharedUseEdit(Asset asset) {
        return AssetUsageChangeHistory.builder()
                .assetId(asset.getId())
                .assetNo(asset.getAssetNo())
                .previousUserName(asset.getActualUserName())
                .previousUserDepartment(asset.getActualUserDepartment())
                .previousUserContact(asset.getActualUserContact())
                .sourceFile("ASSET_EDIT:" + UUID.randomUUID())
                .capturedAt(LocalDateTime.now())
                .build();
    }
}
