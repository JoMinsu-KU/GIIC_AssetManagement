package org.example.giic_asset.dto.disposal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.Disposal;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.DisposalStatus;

@Getter
@Setter
@NoArgsConstructor
public class DisposalRequest {

    @NotNull(message = "자산 ID는 필수입니다.")
    private Long assetId;

    @NotBlank(message = "폐기 사유는 필수입니다.")
    private String reason;

    public Disposal toEntity(Asset asset, User requester) {
        return Disposal.builder()
                .asset(asset)
                .requesterEmployeeNo(requester.getEmployeeNo())
                .requesterName(requester.getName())
                .reason(reason)
                .status(DisposalStatus.PENDING)
                .build();
    }
}
