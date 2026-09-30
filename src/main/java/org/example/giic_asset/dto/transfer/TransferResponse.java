package org.example.giic_asset.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.Transfer;
import org.example.giic_asset.entity.enums.TransferLocationMode;
import org.example.giic_asset.entity.enums.TransferRecipientMode;
import org.example.giic_asset.entity.enums.TransferStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {
    private Long id;
    private Long assetId;
    private String assetName;
    private String assetNo;
    private boolean assetOnly;
    private TransferRecipientMode recipientMode;
    private TransferLocationMode locationMode;

    // === NEW FIELDS ===
    private String fromUserName;
    private String fromUserDepartment;

    private String recipientName;
    private String recipientDepartment;
    private String recipientContact;
    private String recipientEmail;
    private String fromLocation;
    private String transferLocation;
    private String remarks;

    private String requestedByName;
    private String requestedById;
    private String processedByName;
    private String processedById;

    // === DEPRECATED ===
    private String fromUserId; // employeeNo

    private TransferStatus status;
    private String reason;
    private String rejectReason;
    private LocalDateTime requestDate;
    private LocalDateTime processDate;

    public static TransferResponse from(Transfer transfer) {
        TransferRecipientMode recipientMode = resolveRecipientMode(transfer);
        TransferLocationMode locationMode = transfer.getLocationMode() != null
                ? transfer.getLocationMode()
                : TransferLocationMode.CHANGE;

        return TransferResponse.builder()
                .id(transfer.getId())
                .assetId(transfer.getAsset().getId())
                .assetName(transfer.getAsset().getName())
                .assetNo(transfer.getAsset().getAssetNo())
                .assetOnly(recipientMode == TransferRecipientMode.UNASSIGN)
                .recipientMode(recipientMode)
                .locationMode(locationMode)
                // New fields
                .fromUserName(transfer.getFromUserName())
                .fromUserDepartment(transfer.getFromUserDepartment())
                .recipientName(transfer.getRecipientName())
                .recipientDepartment(transfer.getRecipientDepartment())
                .recipientContact(transfer.getRecipientContact())
                .recipientEmail(transfer.getRecipientEmail())
                .fromLocation(transfer.getFromLocation())
                .transferLocation(transfer.getTransferLocation())
                .remarks(transfer.getRemarks())
                .requestedByName(transfer.getRequestedBy() != null ? transfer.getRequestedBy().getName() : null)
                .requestedById(transfer.getRequestedBy() != null ? transfer.getRequestedBy().getEmployeeNo() : null)
                .processedByName(transfer.getProcessedBy() != null ? transfer.getProcessedBy().getName() : null)
                .processedById(transfer.getProcessedBy() != null ? transfer.getProcessedBy().getEmployeeNo() : null)
                // Deprecated (Mapped from old fields if available, or just null)
                // Note: We don't have direct access to old User objects unless we fetch them or
                // they are in the entity
                // Transfer entity has Deprecated fromUser/toUser, so we can use them if they
                // exist
                .fromUserId(transfer.getFromUser() != null ? transfer.getFromUser().getEmployeeNo() : null)
                .status(transfer.getStatus())
                .reason(transfer.getReason())
                .rejectReason(transfer.getRejectReason())
                .requestDate(transfer.getRequestDate())
                .processDate(transfer.getProcessDate())
                .build();
    }

    private static TransferRecipientMode resolveRecipientMode(Transfer transfer) {
        if (transfer.getRecipientMode() != null) {
            return transfer.getRecipientMode();
        }
        return transfer.getRecipientName() == null || transfer.getRecipientName().isBlank()
                ? TransferRecipientMode.UNASSIGN
                : TransferRecipientMode.CHANGE;
    }
}
