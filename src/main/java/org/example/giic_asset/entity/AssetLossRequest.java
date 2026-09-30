package org.example.giic_asset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.giic_asset.entity.enums.LossRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "asset_loss_requests")
public class AssetLossRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "requester_employee_no", nullable = false, length = 20)
    private String requesterEmployeeNo;

    @Column(name = "requester_name", nullable = false, length = 100)
    private String requesterName;

    @Column(name = "loss_date", nullable = false)
    private LocalDate lossDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LossRequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor;

    private LocalDateTime processedAt;

    public void approve(User processor) {
        this.status = LossRequestStatus.APPROVED;
        this.processor = processor;
        this.processedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(User processor, String rejectionReason) {
        this.status = LossRequestStatus.REJECTED;
        this.processor = processor;
        this.processedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }
}
