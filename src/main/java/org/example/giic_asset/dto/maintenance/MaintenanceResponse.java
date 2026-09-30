package org.example.giic_asset.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.Maintenance;
import org.example.giic_asset.entity.enums.MaintenanceStatus;
import org.example.giic_asset.entity.enums.MaintenanceType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaintenanceResponse {
        private Long id;
        private Long assetId;
        private String assetNo;
        private String assetName;

        private String requesterEmployeeNo;
        private String requesterName;

        private MaintenanceType maintenanceType;
        private String description;
        private MaintenanceStatus status;
        private String rejectionReason;

        private LocalDate maintenanceDate;
        private Long cost;
        private String performedBy;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime completedAt;

        public static MaintenanceResponse from(Maintenance maintenance) {
                return MaintenanceResponse.builder()
                                .id(maintenance.getId())
                                .assetId(maintenance.getAsset().getId())
                                .assetNo(maintenance.getAsset().getAssetNo())
                                .assetName(maintenance.getAsset().getName())
                                .requesterEmployeeNo(maintenance.getRequesterEmployeeNo())
                                .requesterName(maintenance.getRequesterName())
                                .maintenanceType(maintenance.getType())
                                .description(maintenance.getDescription())
                                .status(maintenance.getStatus())
                                .rejectionReason(maintenance.getRejectionReason())
                                .maintenanceDate(maintenance.getMaintenanceDate())
                                .cost(maintenance.getCost())
                                .performedBy(maintenance.getPerformedBy())
                                .createdAt(maintenance.getCreatedAt())
                                .updatedAt(maintenance.getUpdatedAt())
                                .completedAt(maintenance.getCompletedAt())
                                .build();
        }
}
