package org.example.giic_asset.dto.maintenance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MaintenanceRejectRequest {
    private String rejectionReason;
}
