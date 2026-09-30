package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.dto.disposal.DisposalRejectRequest;
import org.example.giic_asset.dto.disposal.DisposalRequest;
import org.example.giic_asset.dto.disposal.DisposalResponse;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.DisposalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/disposals")
@RequiredArgsConstructor
public class DisposalController {

    private final DisposalService disposalService;

    // GET /api/disposals - List (permission-based)
    @GetMapping
    public ResponseEntity<List<DisposalResponse>> getDisposalList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== GET /api/disposals ==========");
        log.info("Request by user: {}", userDetails.getUsername());

        List<DisposalResponse> response = disposalService.getDisposalList(
                userDetails.getUsername(),
                userDetails.getAuthorities());

        return ResponseEntity.ok(response);
    }

    // POST /api/disposals - Request disposal
    @PostMapping
    public ResponseEntity<DisposalResponse> requestDisposal(
            @RequestBody @Valid DisposalRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== POST /api/disposals ==========");
        log.info("Request by user: {}, assetId: {}", userDetails.getUsername(), request.getAssetId());

        DisposalResponse response = disposalService.requestDisposal(request, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    // PUT /api/disposals/{id}/approve - @PreAuthorize ADMIN/SUPER
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ResponseEntity<DisposalResponse> approveDisposal(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/disposals/{}/approve ==========", id);
        log.info("Approver: {}", userDetails.getUsername());

        DisposalResponse response = disposalService.approveDisposal(id, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    // PUT /api/disposals/{id}/reject - @PreAuthorize ADMIN/SUPER
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ResponseEntity<DisposalResponse> rejectDisposal(
            @PathVariable Long id,
            @RequestBody DisposalRejectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/disposals/{}/reject ==========", id);
        log.info("Rejecter: {}", userDetails.getUsername());

        DisposalResponse response = disposalService.rejectDisposal(id, userDetails.getUser(),
                request.getRejectionReason());
        return ResponseEntity.ok(response);
    }
}
