package org.example.giic_asset.dto.loss;

import lombok.Builder;
import lombok.Getter;
import org.example.giic_asset.entity.AssetLossRequest;
import org.example.giic_asset.entity.enums.LossRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AssetLossResponse {
    private Long id;
    private Long assetId;
    private String assetNo;
    private String assetName;
    private LocalDate lossDate;
    private String remarks;
    private String requesterEmployeeNo;
    private String requesterName;
    private LossRequestStatus status;
    private String rejectionReason;
    private String processorName;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AssetLossResponse from(AssetLossRequest request) {
        return AssetLossResponse.builder()
                .id(request.getId())
                .assetId(request.getAsset().getId())
                .assetNo(request.getAsset().getAssetNo())
                .assetName(request.getAsset().getName())
                .lossDate(request.getLossDate())
                .remarks(request.getRemarks())
                .requesterEmployeeNo(request.getRequesterEmployeeNo())
                .requesterName(request.getRequesterName())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .processorName(request.getProcessor() != null ? request.getProcessor().getName() : null)
                .processedAt(request.getProcessedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
