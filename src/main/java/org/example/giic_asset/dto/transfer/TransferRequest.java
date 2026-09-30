package org.example.giic_asset.dto.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.enums.TransferLocationMode;
import org.example.giic_asset.entity.enums.TransferRecipientMode;

@Getter
@NoArgsConstructor
public class TransferRequest {
    @NotNull
    private Long assetId;

    private TransferRecipientMode recipientMode;
    private TransferLocationMode locationMode;

    @Deprecated
    private boolean assetOnly;

    // === NEW: Actual user information (can be external) ===
    private String recipientName;
    private String recipientDepartment;
    private String recipientContact;
    private String recipientEmail;

    @NotBlank(message = "이관 장소는 필수 입력 항목입니다.")
    private String transferLocation;
    private String remarks;

    private String reason;
}
