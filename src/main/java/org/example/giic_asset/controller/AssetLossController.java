package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.giic_asset.dto.loss.AssetLossRejectRequest;
import org.example.giic_asset.dto.loss.AssetLossRequestCreate;
import org.example.giic_asset.dto.loss.AssetLossResponse;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.AssetLossService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/losses")
@RequiredArgsConstructor
public class AssetLossController {

    private final AssetLossService assetLossService;

    @GetMapping
    public ResponseEntity<List<AssetLossResponse>> getLossRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(assetLossService.getLossRequests(
                userDetails.getUsername(),
                userDetails.getAuthorities()));
    }

    @PostMapping
    public ResponseEntity<AssetLossResponse> requestLoss(
            @RequestBody @Valid AssetLossRequestCreate request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(assetLossService.requestLoss(request, userDetails.getUser()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ResponseEntity<AssetLossResponse> approveLoss(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(assetLossService.approveLoss(id, userDetails.getUser()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ResponseEntity<AssetLossResponse> rejectLoss(
            @PathVariable Long id,
            @RequestBody @Valid AssetLossRejectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(assetLossService.rejectLoss(
                id,
                userDetails.getUser(),
                request.getRejectionReason()));
    }
}
