package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.asset.*;
import org.example.giic_asset.dto.maintenance.MaintenanceRejectRequest;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.AssetImage;
import org.example.giic_asset.entity.AssetUsageChangeHistory;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.repository.AssetImageRepository;
import org.example.giic_asset.repository.AssetLossRequestRepository;
import org.example.giic_asset.repository.AssetRepository;
import org.example.giic_asset.repository.AssetUsageChangeHistoryRepository;
import org.example.giic_asset.repository.AssetSpecifications;
import org.example.giic_asset.repository.DisposalRepository;
import org.example.giic_asset.repository.MaintenanceRepository;
import org.example.giic_asset.repository.NotificationRepository;
import org.example.giic_asset.repository.TransferRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    public static final int MAX_IMAGES_PER_ASSET = 5;
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    private final AssetRepository assetRepository;
    private final AssetCategoryService categoryService;
    private final AssetUsageChangeHistoryRepository assetUsageChangeHistoryRepository;
    private final AssetImageRepository assetImageRepository;
    private final TransferRepository transferRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DisposalRepository disposalRepository;
    private final AssetLossRequestRepository assetLossRequestRepository;
    private final NotificationRepository notificationRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public Long createAsset(AssetRequest request, User registrar) {
        log.debug("Creating asset: {}", request);
        if (request.getStatus() == AssetStatus.LOST || request.getStatus() == AssetStatus.LOSS_PENDING) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "손망 상태는 승인 절차를 통해서만 지정할 수 있습니다.");
        }
        if (request.getStatus() == AssetStatus.UNUSED && isBlank(request.getLocation())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "미사용 자산은 비치장소가 필요합니다.");
        }
        if (assetRepository.existsByAssetNo(request.getAssetNo())) {
            log.warn("Duplicate asset number: {}", request.getAssetNo());
            throw new BaseException(ErrorCode.DUPLICATE, "이미 존재하는 자산번호입니다.");
        }
        Asset newAsset = request.toEntity(registrar, categoryService.requireActiveSubcategory(request.getCategoryId()));
        if (request.getStatus() == AssetStatus.UNUSED) {
            newAsset.updateActualUser(null, null, null);
            newAsset.setSharedUse(false);
        } else if (newAsset.isSharedUse()) {
            newAsset.updateActualUser(null, null, null);
        }
        Asset savedAsset = assetRepository.save(newAsset);
        log.info("Asset created successfully with ID: {}", savedAsset.getId());
        return savedAsset.getId();
    }

    @Transactional(readOnly = true)
    public Page<AssetResponse> getAssetList(Pageable pageable, String keyword, AssetStatus status, String dept,
            String userId, String actualUserName) {
        AssetSearchCriteria criteria = AssetSearchCriteria.builder()
                .keyword(keyword)
                .status(status)
                .dept(dept)
                .userId(userId)
                .actualUserName(actualUserName)
                .build();
        return getAssetList(pageable, criteria);
    }

    @Transactional(readOnly = true)
    public Page<AssetResponse> getAssetList(Pageable pageable, AssetSearchCriteria criteria) {
        log.debug("AssetService.getAssets() called");

        pageable = AssetPageSort.stable(pageable, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Before query
        long count = assetRepository.count();
        log.debug("Total assets in DB: {}", count);

        Page<Asset> result = assetRepository.findAll(AssetSpecifications.withCriteria(criteria), pageable);
        log.debug("Query result: totalElements={}, totalPages={}, content.size={}",
                result.getTotalElements(), result.getTotalPages(), result.getContent().size());

        // Log each asset
        result.getContent().forEach(asset -> {
            log.debug("Asset: id={}, name={}, status={}, createdAt={}",
                    asset.getId(), asset.getName(), asset.getStatus(), asset.getCreatedAt());
        });

        return result.map(asset -> {
            List<AssetImage> images = assetImageRepository.findOrderedByAssetId(asset.getId());
            List<AssetImageResponse> imageResponses = images.stream().map(AssetImageResponse::from)
                    .collect(Collectors.toList());
            return AssetResponse.from(asset, imageResponses);
        });
    }

    @Transactional(readOnly = true)
    public AssetResponse getAssetDetail(Long assetId) {
        log.debug("Fetching asset details for ID: {}", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> {
                    log.error("Asset not found with ID: {}", assetId);
                    return new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다.");
                });
        List<AssetImage> images = assetImageRepository.findOrderedByAssetId(assetId);
        List<AssetImageResponse> imageResponses = images.stream().map(AssetImageResponse::from)
                .collect(Collectors.toList());
        return AssetResponse.from(asset, imageResponses);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctLocations() {
        return assetRepository.findDistinctLocations();
    }

    @Transactional
    public void deleteAsset(Long assetId) {
        log.debug("Deleting asset with ID: {}", assetId);
        if (!assetRepository.existsById(assetId)) {
            log.error("Asset not found for deletion with ID: {}", assetId);
            throw new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다.");
        }

        List<AssetImage> images = assetImageRepository.findOrderedByAssetId(assetId);

        // Delete related records (Cascade delete manually)
        notificationRepository.deleteByRelatedEntityTypeAndRelatedEntityId("ASSET", assetId);
        assetLossRequestRepository.deleteByAssetId(assetId);
        disposalRepository.deleteByAssetId(assetId);
        transferRepository.deleteByAssetId(assetId);
        maintenanceRepository.deleteByAssetId(assetId);
        assetImageRepository.deleteByAssetId(assetId);

        assetRepository.deleteById(assetId);
        deleteFilesAfterCommit(images);
        log.info("Asset deleted successfully: {}", assetId);
    }

    @Transactional
    public void updateAsset(Long assetId, AssetUpdateRequest request) {
        log.debug("Updating asset with ID: {}, request: {}", assetId, request);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> {
                    log.error("Asset not found for update with ID: {}", assetId);
                    return new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다.");
                });
        AssetStatus requestedStatus = request.getStatus();
        AssetStatus nextStatus = requestedStatus == null ? asset.getStatus() : requestedStatus;
        if (requestedStatus != null
                && requestedStatus != asset.getStatus()
                && (requestedStatus == AssetStatus.LOST || requestedStatus == AssetStatus.LOSS_PENDING)) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "손망 상태는 승인 절차를 통해서만 지정할 수 있습니다.");
        }
        if (nextStatus == AssetStatus.UNUSED
                && isBlank(request.getLocation() == null ? asset.getLocation() : request.getLocation())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "미사용 자산은 비치장소가 필요합니다.");
        }
        if (asset.getStatus() == AssetStatus.UNUSED && nextStatus == AssetStatus.NORMAL
                && !Boolean.TRUE.equals(request.getSharedUse()) && isBlank(request.getActualUserName())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "사용중으로 변경하려면 실사용자 이름을 입력해 주세요.");
        }
        if (Boolean.TRUE.equals(request.getSharedUse()) && !asset.isSharedUse()
                && (asset.getStatus() != AssetStatus.NORMAL || nextStatus != AssetStatus.NORMAL)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "정상 자산만 공용 자산으로 변경할 수 있습니다.");
        }
        if (Boolean.TRUE.equals(request.getSharedUse()) && !asset.isSharedUse()) {
            assetUsageChangeHistoryRepository.save(AssetUsageChangeHistory.forSharedUseEdit(asset));
        }
        asset.update(request.getName(), request.getModel(), request.getSpec(), request.getPurchaseDate(),
                request.getValue(), request.getManagingDept(), request.getLocation(), request.getStatus());
        if (request.getCategoryId() != null) {
            asset.setCategory(categoryService.requireActiveSubcategory(request.getCategoryId()));
        }

        if (nextStatus == AssetStatus.UNUSED) {
            asset.updateActualUser(null, null, null);
            asset.setSharedUse(false);
        } else if (Boolean.TRUE.equals(request.getSharedUse())) {
            asset.setSharedUse(true);
        } else if (asset.isSharedUse() && request.getSharedUse() == null) {
            // Older clients omit sharedUse; do not turn an imported shared asset into an assigned one.
            asset.updateActualUser(null, null, null);
        } else if (request.getActualUserName() != null ||
                request.getActualUserDepartment() != null ||
                request.getActualUserContact() != null) {
            if (Boolean.FALSE.equals(request.getSharedUse()) || !isBlank(request.getActualUserName())) {
                asset.setSharedUse(false);
            }
            asset.updateActualUser(
                    request.getActualUserName(),
                    request.getActualUserDepartment(),
                    request.getActualUserContact());
        } else if (Boolean.FALSE.equals(request.getSharedUse())) {
            asset.setSharedUse(false);
        }
    }

    @Transactional
    public void uploadImage(Long assetId, MultipartFile file, boolean isPrimary) {
        log.debug("Replacing images for asset ID: {}", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> {
                    log.error("Asset not found for image upload with ID: {}", assetId);
                    return new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다.");
                });

        validateImage(file);
        List<AssetImage> existingImages = assetImageRepository.findOrderedByAssetId(assetId);
        StoredImage storedImage = storeImage(file);

        try {
            if (!existingImages.isEmpty()) {
                assetImageRepository.deleteAll(existingImages);
            }

            AssetImage image = AssetImage.builder()
                    .asset(asset)
                    .imageUrl(storedImage.imageUrl())
                    .isPrimary(true)
                    .build();
            assetImageRepository.save(image);
            deleteFilesAfterCommit(existingImages);
            registerRollbackCleanup(List.of(storedImage.path()));
        } catch (RuntimeException e) {
            deleteStoredPath(storedImage.path());
            throw e;
        }

        log.info("Legacy image replacement completed for asset ID: {}, requestedPrimary={}", assetId, isPrimary);
    }

    @Transactional
    public List<AssetImageResponse> addImages(Long assetId, List<MultipartFile> files, Integer primaryIndex) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다."));

        if (files == null || files.isEmpty()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "추가할 이미지를 선택해 주세요.");
        }

        if (primaryIndex != null && (primaryIndex < 0 || primaryIndex >= files.size())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "대표 이미지 위치가 올바르지 않습니다.");
        }

        List<AssetImage> existingImages = assetImageRepository.findOrderedByAssetId(assetId);
        if (existingImages.size() + files.size() > MAX_IMAGES_PER_ASSET) {
            throw new BaseException(ErrorCode.BAD_REQUEST,
                    "자산 이미지는 최대 " + MAX_IMAGES_PER_ASSET + "장까지 등록할 수 있습니다.");
        }

        files.forEach(this::validateImage);

        boolean hasPrimary = existingImages.stream().anyMatch(AssetImage::isPrimary);
        if (primaryIndex != null) {
            existingImages.forEach(image -> image.updatePrimary(false));
        }

        List<StoredImage> storedImages = new ArrayList<>();
        List<AssetImage> newImages = new ArrayList<>();

        try {
            for (int index = 0; index < files.size(); index++) {
                StoredImage storedImage = storeImage(files.get(index));
                storedImages.add(storedImage);

                boolean primary = primaryIndex != null
                        ? primaryIndex == index
                        : !hasPrimary && index == 0;

                newImages.add(AssetImage.builder()
                        .asset(asset)
                        .imageUrl(storedImage.imageUrl())
                        .isPrimary(primary)
                        .build());
            }

            assetImageRepository.saveAll(newImages);
            assetImageRepository.flush();
            registerRollbackCleanup(storedImages.stream().map(StoredImage::path).toList());
        } catch (RuntimeException e) {
            storedImages.forEach(storedImage -> deleteStoredPath(storedImage.path()));
            throw e;
        }

        log.info("Added {} images to asset ID: {}", newImages.size(), assetId);
        return getImageResponses(assetId);
    }

    @Transactional
    public void setPrimaryImage(Long assetId, Long imageId) {
        List<AssetImage> images = assetImageRepository.findOrderedByAssetId(assetId);
        AssetImage targetImage = images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산 이미지를 찾을 수 없습니다."));

        images.forEach(image -> image.updatePrimary(image.getId().equals(targetImage.getId())));
        log.info("Primary image changed: assetId={}, imageId={}", assetId, imageId);
    }

    @Transactional
    public void deleteImage(Long assetId, Long imageId) {
        AssetImage targetImage = assetImageRepository.findByIdAndAssetId(imageId, assetId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산 이미지를 찾을 수 없습니다."));
        boolean wasPrimary = targetImage.isPrimary();

        assetImageRepository.delete(targetImage);

        if (wasPrimary) {
            assetImageRepository.findOrderedByAssetId(assetId).stream()
                    .filter(image -> !image.getId().equals(imageId))
                    .findFirst()
                    .ifPresent(image -> image.updatePrimary(true));
        }

        deleteFilesAfterCommit(List.of(targetImage));
        log.info("Image deleted: assetId={}, imageId={}", assetId, imageId);
    }

    private List<AssetImageResponse> getImageResponses(Long assetId) {
        return assetImageRepository.findOrderedByAssetId(assetId).stream()
                .map(AssetImageResponse::from)
                .collect(Collectors.toList());
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "비어 있는 이미지 파일은 등록할 수 없습니다.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BaseException(ErrorCode.IMAGE_TOO_LARGE, "이미지 한 장의 최대 크기는 10MB입니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_FORMAT, "JPG, PNG, WEBP 이미지만 등록할 수 있습니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private StoredImage storeImage(MultipartFile file) {
        String extension = getImageExtension(file);
        String fileName = UUID.randomUUID() + extension;
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetPath = uploadRoot.resolve(fileName).normalize();

        if (!targetPath.startsWith(uploadRoot)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "올바르지 않은 이미지 파일명입니다.");
        }

        try {
            Files.createDirectories(uploadRoot);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath);
            }
            return new StoredImage(targetPath, "/uploads/" + fileName);
        } catch (IOException e) {
            log.error("Error saving image file: {}", targetPath, e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 파일 저장 중 오류가 발생했습니다.");
        }
    }

    private String getImageExtension(MultipartFile file) {
        return switch (file.getContentType().toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private void deleteFilesAfterCommit(List<AssetImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        Runnable deleteAction = () -> images.forEach(image -> deleteStoredFile(image.getImageUrl()));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteAction.run();
                }
            });
        } else {
            deleteAction.run();
        }
    }

    private void registerRollbackCleanup(List<Path> storedPaths) {
        if (storedPaths.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    storedPaths.forEach(AssetService.this::deleteStoredPath);
                }
            }
        });
    }

    private void deleteStoredFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        Path fileName = Paths.get(imageUrl).getFileName();
        if (fileName == null) {
            return;
        }

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetPath = uploadRoot.resolve(fileName.toString()).normalize();
        if (targetPath.startsWith(uploadRoot)) {
            deleteStoredPath(targetPath);
        }
    }

    private void deleteStoredPath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete stored image file: {}", path, e);
        }
    }

    private record StoredImage(Path path, String imageUrl) {
    }

    // === DISPOSAL METHODS ===
    @Transactional
    public void requestDisposal(Long assetId, DisposalRequest request, User requester) {
        log.debug("Requesting disposal for asset: {}", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다."));

        // Add validation
        if (asset.getStatus() == AssetStatus.DISPOSED || asset.getStatus() == AssetStatus.DISPOSAL_PENDING) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "이미 폐기되었거나 폐기 신청 중인 자산입니다.");
        }

        asset.requestDisposal(request.getDisposalType(), request.getReason(), requester.getEmployeeNo());
    }

    @Transactional
    public void approveDisposal(Long assetId) {
        log.debug("Approving disposal for asset: {}", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다."));

        if (asset.getStatus() != AssetStatus.DISPOSAL_PENDING) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "폐기 신청 상태가 아닙니다.");
        }

        asset.approveDisposal();
    }

    @Transactional
    public void rejectDisposal(Long assetId, MaintenanceRejectRequest request) {
        log.debug("Rejecting disposal for asset: {}", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다."));

        if (asset.getStatus() != AssetStatus.DISPOSAL_PENDING) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "폐기 신청 상태가 아닙니다.");
        }

        asset.rejectDisposal(request.getRejectionReason());
    }

}
