package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.dto.ApiResponse;
import org.example.giic_asset.common.dto.PageResponse;
import org.example.giic_asset.dto.asset.AssetExportRequest;
import org.example.giic_asset.dto.asset.AssetImageResponse;
import org.example.giic_asset.dto.asset.AssetRequest;
import org.example.giic_asset.dto.asset.AssetResponse;
import org.example.giic_asset.dto.asset.AssetSearchCriteria;
import org.example.giic_asset.dto.asset.AssetUpdateRequest;
import org.example.giic_asset.dto.asset.DisposalRequest;
import org.example.giic_asset.dto.maintenance.MaintenanceRejectRequest;
import org.example.giic_asset.dto.ocr.OcrResponse;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.AssetService;
import org.example.giic_asset.service.AssetExcelExportService;
import org.example.giic_asset.service.GeminiOcrService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetExcelExportService assetExcelExportService;
    private final GeminiOcrService geminiOcrService;

    @PostMapping
    public ApiResponse<Long> createAsset(
            @RequestBody @Valid AssetRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Request to create asset: {}", request.getAssetNo());
        Long assetId = assetService.createAsset(request, userDetails.getUser());
        return ApiResponse.success(assetId, "자산이 등록되었습니다.");
    }

    @GetMapping
    public ApiResponse<PageResponse<AssetResponse>> getAssetList(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String dept,
            @RequestParam(name = "user_id", required = false) String userId,
            @RequestParam(required = false) String actualUserName,
            @RequestParam(required = false) String assetNo,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String location,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDateTo,
            @RequestParam(required = false) Long valueMin,
            @RequestParam(required = false) Long valueMax) {
        log.info("========== GET /api/assets ==========");
        log.info("Request params: page={}, size={}, keyword={}, status={}, dept={}, userId={}, actualUserName={}",
                pageable.getPageNumber(), pageable.getPageSize(), keyword, status, dept, userId, actualUserName);

        try {
            AssetSearchCriteria criteria = AssetSearchCriteria.builder()
                    .keyword(keyword)
                    .status(status)
                    .dept(dept)
                    .userId(userId)
                    .actualUserName(actualUserName)
                    .assetNo(assetNo)
                    .name(name)
                    .model(model)
                    .location(location)
                    .purchaseDateFrom(purchaseDateFrom)
                    .purchaseDateTo(purchaseDateTo)
                    .valueMin(valueMin)
                    .valueMax(valueMax)
                    .build();
            Page<AssetResponse> result = assetService.getAssetList(pageable, criteria);
            log.info("Response: total_elements={}, total_pages={}, data_count={}",
                    result.getTotalElements(), result.getTotalPages(), result.getContent().size());
            return ApiResponse.success(PageResponse.of(result));
        } catch (Exception e) {
            log.error("Error fetching assets: ", e);
            throw e;
        }
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportAssets(@RequestBody AssetExportRequest request) {
        byte[] workbook = assetExcelExportService.export(request.getSafeFilters(), request.getColumns());
        String filename = "asset-list_"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(workbook.length)
                .body(workbook);
    }

    @PostMapping("/export/preview")
    public ApiResponse<PageResponse<AssetResponse>> previewExport(
            @RequestBody AssetExportRequest request,
            @PageableDefault(size = 20, sort = "assetNo") Pageable pageable) {
        Page<AssetResponse> result = assetExcelExportService.preview(request.getSafeFilters(), pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @GetMapping("/locations")
    public ApiResponse<List<String>> getAssetLocations() {
        return ApiResponse.success(assetService.getDistinctLocations());
    }

    @GetMapping("/{assetId}")
    public ApiResponse<AssetResponse> getAssetDetail(@PathVariable Long assetId) {
        log.info("Request to get asset detail: {}", assetId);
        return ApiResponse.success(assetService.getAssetDetail(assetId));
    }

    @PutMapping("/{assetId}")
    public ApiResponse<Void> updateAsset(@PathVariable Long assetId, @RequestBody AssetUpdateRequest request) {
        log.info("Request to update asset: {}", assetId);
        assetService.updateAsset(assetId, request);
        return ApiResponse.success("자산 정보가 수정되었습니다.");
    }

    @DeleteMapping("/{assetId}")
    public ApiResponse<Void> deleteAsset(@PathVariable Long assetId) {
        log.info("Request to delete asset: {}", assetId);
        assetService.deleteAsset(assetId);
        return ApiResponse.success("자산이 삭제되었습니다.");
    }

    @PostMapping("/{assetId}/images")
    public ApiResponse<Void> uploadImage(
            @PathVariable Long assetId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "is_primary", required = false, defaultValue = "false") boolean isPrimary) {
        log.info("Request to upload image for asset: {}, filename: {}", assetId, file.getOriginalFilename());
        assetService.uploadImage(assetId, file, isPrimary);
        return ApiResponse.success("이미지가 업로드되었습니다.");
    }

    @PostMapping("/{assetId}/images/batch")
    public ApiResponse<List<AssetImageResponse>> addImages(
            @PathVariable Long assetId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "primary_index", required = false) Integer primaryIndex) {
        log.info("Request to add images: assetId={}, count={}, primaryIndex={}",
                assetId, files.size(), primaryIndex);
        return ApiResponse.success(
                assetService.addImages(assetId, files, primaryIndex),
                "자산 이미지가 추가되었습니다.");
    }

    @PutMapping("/{assetId}/images/{imageId}/primary")
    public ApiResponse<Void> setPrimaryImage(
            @PathVariable Long assetId,
            @PathVariable Long imageId) {
        assetService.setPrimaryImage(assetId, imageId);
        return ApiResponse.success("대표 이미지가 변경되었습니다.");
    }

    @DeleteMapping("/{assetId}/images/{imageId}")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long assetId,
            @PathVariable Long imageId) {
        assetService.deleteImage(assetId, imageId);
        return ApiResponse.success("자산 이미지가 삭제되었습니다.");
    }

    // === DISPOSAL ENDPOINTS ===
    @PostMapping("/{assetId}/disposal")
    public ApiResponse<Void> requestDisposal(
            @PathVariable Long assetId,
            @RequestBody DisposalRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Request to dispose asset: {}, user: {}", assetId, userDetails.getUsername());
        assetService.requestDisposal(assetId, request, userDetails.getUser());
        return ApiResponse.success("폐기 신청이 완료되었습니다.");
    }

    @PutMapping("/{assetId}/disposal/approve")
    public ApiResponse<Void> approveDisposal(@PathVariable Long assetId) {
        log.info("Request to approve disposal for asset: {}", assetId);
        assetService.approveDisposal(assetId);
        return ApiResponse.success("폐기 신청이 승인되었습니다.");
    }

    @PutMapping("/{assetId}/disposal/reject")
    public ApiResponse<Void> rejectDisposal(
            @PathVariable Long assetId,
            @RequestBody MaintenanceRejectRequest request) {
        log.info("Request to reject disposal for asset: {}", assetId);
        assetService.rejectDisposal(assetId, request);
        return ApiResponse.success("폐기 신청이 거절되었습니다.");
    }

    @PostMapping("/ocr")
    public ApiResponse<OcrResponse> ocrImage(@RequestParam("file") MultipartFile file) {
        log.info("Request to OCR image: filename={}, size={}", file.getOriginalFilename(), file.getSize());
        OcrResponse response = geminiOcrService.processOcr(file);
        System.out.println(response);
        return ApiResponse.success(response, "OCR 처리가 완료되었습니다.");
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
