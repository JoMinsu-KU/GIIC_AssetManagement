package org.example.giic_asset.dto.disposal;

import lombok.Builder;
import lombok.Getter;
import org.example.giic_asset.entity.Disposal;
import org.example.giic_asset.entity.enums.DisposalStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class DisposalResponse {

    private Long id;
    private Long assetId;
    private String assetNo;
    private String assetName;

    private String requesterEmployeeNo;
    private String requesterName;

    private String reason;
    private DisposalStatus status;
    private String rejectionReason;

    private String processorName;
    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DisposalResponse from(Disposal disposal) {
        return DisposalResponse.builder()
                .id(disposal.getId())
                .assetId(disposal.getAsset().getId())
                .assetNo(disposal.getAsset().getAssetNo())
                .assetName(disposal.getAsset().getName())
                .requesterEmployeeNo(disposal.getRequesterEmployeeNo())
                .requesterName(disposal.getRequesterName())
                .reason(disposal.getReason())
                .status(disposal.getStatus())
                .rejectionReason(disposal.getRejectionReason())
                .processorName(disposal.getProcessor() != null ? disposal.getProcessor().getName() : null)
                .processedAt(disposal.getProcessedAt())
                .createdAt(disposal.getCreatedAt())
                .updatedAt(disposal.getUpdatedAt())
                .build();
    }
}
