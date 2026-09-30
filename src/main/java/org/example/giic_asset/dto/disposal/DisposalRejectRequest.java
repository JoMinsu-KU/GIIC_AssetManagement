package org.example.giic_asset.dto.disposal;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DisposalRejectRequest {

    @NotBlank(message = "거절 사유는 필수입니다.")
    private String rejectionReason;
}
