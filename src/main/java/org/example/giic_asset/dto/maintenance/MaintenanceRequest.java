package org.example.giic_asset.dto.maintenance;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.Maintenance;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.MaintenanceStatus;
import org.example.giic_asset.entity.enums.MaintenanceType;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class MaintenanceRequest {
    @NotNull
    private Long assetId;

    @NotNull
    private MaintenanceType maintenanceType;

    private String description;

    // Optional fields for flexibility
    private LocalDate maintenanceDate;
    private Long cost;
    private String performedBy;

    public Maintenance toEntity(Asset asset, User requester) {
        return Maintenance.builder()
                .asset(asset)
                .requester(requester)
                .requesterEmployeeNo(requester.getEmployeeNo())
                .requesterName(requester.getName())
                .type(maintenanceType)
                .description(description)
                .maintenanceDate(maintenanceDate != null ? maintenanceDate : LocalDate.now())
                .status(MaintenanceStatus.PENDING)
                .cost(cost)
                .performedBy(performedBy)
                .registeredBy(requester)
                .build();
    }
}
