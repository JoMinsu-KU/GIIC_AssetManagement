package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.common.dto.ApiResponse;
import org.example.giic_asset.dto.maintenance.MaintenanceRejectRequest;
import org.example.giic_asset.dto.maintenance.MaintenanceRequest;
import org.example.giic_asset.dto.maintenance.MaintenanceResponse;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.MaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/maintenances")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    // GET /api/maintenances - List (permission-based)
    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> getMaintenanceList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== GET /api/maintenances ==========");
        log.info("Request by user: {}", userDetails.getUsername());

        List<MaintenanceResponse> response = maintenanceService.getMaintenanceList(
                userDetails.getUsername(),
                userDetails.getAuthorities());

        return ResponseEntity.ok(response);
    }

    // POST /api/maintenances - Create request (any authenticated user)
    @PostMapping
    public ResponseEntity<MaintenanceResponse> requestMaintenance(
            @RequestBody @Valid MaintenanceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== POST /api/maintenances ==========");
        log.info("Request by user: {}, assetId: {}", userDetails.getUsername(), request.getAssetId());

        MaintenanceResponse response = maintenanceService.requestMaintenance(request, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    // PUT /api/maintenances/{id}/approve - ADMIN/SUPER only
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ResponseEntity<MaintenanceResponse> approveMaintenance(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/maintenances/{}/approve ==========", id);
        log.info("Approver: {}", userDetails.getUsername());

        MaintenanceResponse response = maintenanceService.approveMaintenance(id, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    // PUT /api/maintenances/{id}/complete - ADMIN/SUPER only
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ResponseEntity<MaintenanceResponse> completeMaintenance(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/maintenances/{}/complete ==========", id);
        log.info("Completer: {}", userDetails.getUsername());

        MaintenanceResponse response = maintenanceService.completeMaintenance(id, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    // PUT /api/maintenances/{id}/reject - ADMIN/SUPER only
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ResponseEntity<MaintenanceResponse> rejectMaintenance(
            @PathVariable Long id,
            @RequestBody MaintenanceRejectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/maintenances/{}/reject ==========", id);
        log.info("Rejecter: {}", userDetails.getUsername());

        MaintenanceResponse response = maintenanceService.rejectMaintenance(id, userDetails.getUser(),
                request.getRejectionReason());
        return ResponseEntity.ok(response);
    }
}
