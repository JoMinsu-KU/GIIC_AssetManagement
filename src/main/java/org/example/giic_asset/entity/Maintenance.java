package org.example.giic_asset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.giic_asset.entity.enums.MaintenanceStatus;
import org.example.giic_asset.entity.enums.MaintenanceType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "maintenance")
public class Maintenance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    // The user who requested the maintenance
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "requester_employee_no")
    private String requesterEmployeeNo;

    @Column(name = "requester_name")
    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Admin who processed (approved/completed/rejected)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    private LocalDate maintenanceDate; // Planned date?
    private LocalDateTime completedAt; // Actual completion timestamp

    // Legacy fields - kept but might be unused if we switch to description
    private String reason;
    private String details;
    private Long cost;
    private String performedBy;

    // === Methods for state changes ===

    public void approve(User processor) {
        this.status = MaintenanceStatus.IN_PROGRESS;
        this.processor = processor;
    }

    public void complete(User processor) {
        this.status = MaintenanceStatus.COMPLETED;
        this.processor = processor;
        this.completedAt = LocalDateTime.now();
    }

    public void reject(User processor, String rejectionReason) {
        this.status = MaintenanceStatus.REJECTED;
        this.processor = processor;
        this.rejectionReason = rejectionReason;
        this.completedAt = LocalDateTime.now(); // Rejected is also a form of completion
    }

    public void update(LocalDate maintenanceDate, MaintenanceType type, String description, Long cost,
            String performedBy) {
        if (maintenanceDate != null)
            this.maintenanceDate = maintenanceDate;
        if (type != null)
            this.type = type;
        if (description != null)
            this.description = description;
        if (cost != null)
            this.cost = cost;
        if (performedBy != null)
            this.performedBy = performedBy;
    }
}
