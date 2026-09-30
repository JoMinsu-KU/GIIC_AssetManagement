package org.example.giic_asset.dto.loss;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssetLossRejectRequest {

    @NotBlank(message = "반려 사유는 필수입니다.")
    private String rejectionReason;
}
