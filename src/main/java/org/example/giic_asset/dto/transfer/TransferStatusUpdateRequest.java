package org.example.giic_asset.dto.transfer;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.enums.TransferStatus;

@Getter
@NoArgsConstructor
public class TransferStatusUpdateRequest {
    @NotNull
    private TransferStatus status;
    private String comment;
    private String reason;
}
