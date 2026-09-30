package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.dto.ApiResponse;
import org.example.giic_asset.common.dto.PageResponse;
import org.example.giic_asset.dto.transfer.TransferRequest;
import org.example.giic_asset.dto.transfer.TransferResponse;
import org.example.giic_asset.entity.enums.TransferStatus;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.TransferService;
import org.springframework.data.domain.Page;
import org.example.giic_asset.dto.transfer.TransferStatusUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ApiResponse<TransferResponse> createTransfer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid TransferRequest request) {
        log.info("========== POST /api/transfers ==========");
        log.info("Request to create transfer for assetId: {}, by user: {}", request.getAssetId(),
                userDetails.getUsername());
        try {
            TransferResponse response = transferService.createTransfer(request, userDetails.getUser());
            log.info("Transfer created successfully");
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Failed to create transfer: ", e);
            throw e;
        }
    }

    @GetMapping("/my")
    public ApiResponse<List<TransferResponse>> getMyTransfers(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== GET /api/transfers/my ==========");
        return ApiResponse.success(transferService.getMyTransfers(userDetails.getUser()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ApiResponse<List<TransferResponse>> getAllTransfers() {
        log.info("========== GET /api/transfers/all ==========");
        return ApiResponse.success(transferService.getAllTransfers());
    }

    @GetMapping("/asset/{assetId}")
    public ApiResponse<List<TransferResponse>> getAssetTransferHistory(@PathVariable Long assetId) {
        log.info("========== GET /api/transfers/asset/{} ==========", assetId);
        return ApiResponse.success(transferService.getAssetTransferHistory(assetId));
    }

    @GetMapping
    public ApiResponse<PageResponse<TransferResponse>> getTransferList(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "received") String type,
            @RequestParam(required = false) TransferStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("========== GET /api/transfers ==========");
        log.info("Request params: type={}, status={}, page={}, size={}, user={}",
                type, status, pageable.getPageNumber(), pageable.getPageSize(), userDetails.getUsername());
        try {
            Page<TransferResponse> result = transferService.getTransferList(userDetails.getUsername(), type, status,
                    pageable);
            log.info("Response: total_elements={}, data_count={}",
                    result.getTotalElements(), result.getContent().size());
            return ApiResponse.success(PageResponse.of(result));
        } catch (Exception e) {
            log.error("Failed to fetch transfer list: ", e);
            throw e;
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ApiResponse<TransferResponse> approveTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/transfers/{}/approve ==========", id);
        log.info("Approver: {}", userDetails.getUsername());
        try {
            TransferResponse response = transferService.approveTransfer(id, userDetails.getUser());
            log.info("Transfer approved successfully");
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Failed to approve transfer: ", e);
            throw e;
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
    public ApiResponse<TransferResponse> rejectTransfer(
            @PathVariable Long id,
            @RequestBody TransferStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/transfers/{}/reject ==========", id);
        log.info("Rejecter: {}", userDetails.getUsername());
        try {
            TransferResponse response = transferService.rejectTransfer(id, userDetails.getUser(), request.getReason());
            log.info("Transfer rejected successfully");
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Failed to reject transfer: ", e);
            throw e;
        }
    }
}
