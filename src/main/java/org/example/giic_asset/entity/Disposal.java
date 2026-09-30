package org.example.giic_asset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.giic_asset.entity.enums.DisposalStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "disposal")
public class Disposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "requester_employee_no")
    private String requesterEmployeeNo;

    @Column(name = "requester_name")
    private String requesterName;

    @Column(columnDefinition = "TEXT")
    private String reason; // 폐기 사유

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisposalStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Admin who processed (approved/rejected)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor;

    private LocalDateTime processedAt;

    // === Methods for state changes ===

    public void approve(User processor) {
        this.status = DisposalStatus.APPROVED;
        this.processor = processor;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(User processor, String rejectionReason) {
        this.status = DisposalStatus.REJECTED;
        this.processor = processor;
        this.rejectionReason = rejectionReason;
        this.processedAt = LocalDateTime.now();
    }
}
