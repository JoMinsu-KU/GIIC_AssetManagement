package org.example.giic_asset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.giic_asset.entity.enums.TransferLocationMode;
import org.example.giic_asset.entity.enums.TransferRecipientMode;
import org.example.giic_asset.entity.enums.TransferStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "transfers")
public class Transfer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    // === ACTUAL USERS (실사용자) - Can be external ===
    @Column(length = 100)
    private String fromUserName; // Previous actual user name

    @Column(length = 100)
    private String fromUserDepartment; // Previous user's department

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_department")
    private String recipientDepartment;

    @Column(name = "recipient_contact")
    private String recipientContact;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_mode", length = 20)
    private TransferRecipientMode recipientMode;

    @Column(name = "from_location")
    private String fromLocation;

    @Column(name = "transfer_location", nullable = false)
    private String transferLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_mode", length = 20)
    private TransferLocationMode locationMode;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    // === SYSTEM USERS (시스템 사용자) - Who manages the transfer ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy; // System user who initiated this transfer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy; // Admin who approved/rejected

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    private String reason;
    private String rejectReason;
    private LocalDateTime requestDate;
    private LocalDateTime processDate;

    // === DEPRECATED (Keep for backward compatibility) ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    @Deprecated
    private User fromUser;

    public void approve(User approver) {
        this.status = TransferStatus.APPROVED;
        this.processedBy = approver;
        this.processDate = LocalDateTime.now();
    }

    public void reject(User rejecter, String reason) {
        this.status = TransferStatus.REJECTED;
        this.processedBy = rejecter;
        this.processDate = LocalDateTime.now();
        this.rejectReason = reason;
    }
}
