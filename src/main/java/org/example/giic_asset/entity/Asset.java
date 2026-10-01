package org.example.giic_asset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.giic_asset.entity.enums.AssetStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "assets")
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String assetNo;

    @Column(nullable = false)
    private String name;

    private String model;

    private String spec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AssetCategory category;

    private LocalDate purchaseDate;

    private Long value;

    private String managingDept;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    @Column(nullable = false)
    @Builder.Default
    private boolean sharedUse = false;

    // === NEW FIELDS FOR ACTUAL USER (실사용자) ===
    @Column(length = 100)
    private String actualUserName; // Can be anyone - internal or external

    @Column(length = 100)
    private String actualUserDepartment; // Department/organization

    @Column(length = 100)
    private String actualUserContact; // Phone or email for contact

    // === NEW FIELD FOR REGISTRAR (자산등록자) ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy; // System user who created this asset record

    // === DISPOSAL FIELDS ===
    @Column(length = 50)
    private String disposalType;

    @Column(columnDefinition = "TEXT")
    private String disposalReason;

    private LocalDateTime disposalRequestDate;

    @Column(length = 20)
    private String disposalRequesterId;

    @Column(columnDefinition = "TEXT")
    private String disposalRejectionReason;

    // === LOSS FIELDS ===
    private LocalDate lossDate;

    @Column(length = 20)
    private String lossRequesterId;

    @Column(length = 100)
    private String lossRequesterName;

    @Column(columnDefinition = "TEXT")
    private String lossRemarks;

    // === DEPRECATED (Keep for backward compatibility) ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_user_id")
    @Deprecated
    private User user;

    public void requestDisposal(String type, String reason, String requesterId) {
        this.status = AssetStatus.DISPOSAL_PENDING;
        this.disposalType = type;
        this.disposalReason = reason;
        this.disposalRequesterId = requesterId;
        this.disposalRequestDate = LocalDateTime.now();
        this.disposalRejectionReason = null;
    }

    public void approveDisposal() {
        this.status = AssetStatus.DISPOSED;
    }

    public void confirmDisposal(LocalDateTime requestDate, String reason, String requesterId) {
        this.status = AssetStatus.DISPOSED;
        this.disposalRequestDate = requestDate;
        this.disposalReason = reason;
        this.disposalRequesterId = requesterId;
        this.disposalRejectionReason = null;
    }

    public void rejectDisposal(String reason) {
        this.status = AssetStatus.NORMAL;
        this.disposalRejectionReason = reason;
    }

    public void requestLoss() {
        this.status = AssetStatus.LOSS_PENDING;
    }

    public void confirmLoss(LocalDate lossDate, String requesterId, String requesterName, String remarks) {
        this.status = AssetStatus.LOST;
        this.lossDate = lossDate;
        this.lossRequesterId = requesterId;
        this.lossRequesterName = requesterName;
        this.lossRemarks = remarks;
    }

    public void rejectLoss() {
        this.status = AssetStatus.NORMAL;
    }

    public void update(String name, String model, String spec, LocalDate purchaseDate, Long value, String managingDept,
            String location, AssetStatus status) {
        if (name != null)
            this.name = name;
        this.model = model;
        this.spec = spec;
        this.purchaseDate = purchaseDate;
        this.value = value;
        this.managingDept = managingDept;
        if (location != null)
            this.location = location;
        if (status != null)
            this.status = status;
    }

    public void updateActualUser(String name, String department, String contact) {
        this.actualUserName = name;
        this.actualUserDepartment = department;
        this.actualUserContact = contact;
    }

    public void setSharedUse(boolean sharedUse) {
        this.sharedUse = sharedUse;
        if (sharedUse) {
            updateActualUser(null, null, null);
        }
    }

    public void markTransferPending() {
        this.status = AssetStatus.TRANSFER_PENDING;
    }

    public void completeTransfer(String userName, String userDepartment, String userContact, String location) {
        this.actualUserName = userName;
        this.actualUserDepartment = userDepartment;
        this.actualUserContact = userContact;
        this.location = location;
        this.status = AssetStatus.NORMAL;
    }

    public void cancelTransfer() {
        this.status = AssetStatus.NORMAL;
    }

    public void setRegistrar(User registrar) {
        this.registeredBy = registrar;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public void setCategory(AssetCategory category) {
        this.category = category;
    }

    @Deprecated
    public void changeUser(User user) {
        this.user = user;
    }
}
