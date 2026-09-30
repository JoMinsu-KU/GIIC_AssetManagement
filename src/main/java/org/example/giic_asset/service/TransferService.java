package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.transfer.TransferRequest;
import org.example.giic_asset.dto.transfer.TransferResponse;
import org.example.giic_asset.dto.transfer.TransferStatusUpdateRequest;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.Transfer;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.entity.enums.TransferLocationMode;
import org.example.giic_asset.entity.enums.TransferRecipientMode;
import org.example.giic_asset.entity.enums.TransferStatus;
import org.example.giic_asset.repository.AssetRepository;
import org.example.giic_asset.repository.TransferRepository;
import org.example.giic_asset.repository.UserRepository;
import org.example.giic_asset.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

        private final TransferRepository transferRepository;
        private final AssetRepository assetRepository;
        private final UserRepository userRepository;
        private final NotificationService notificationService;

        @Transactional
        public TransferResponse createTransfer(TransferRequest request, User requestor) {
                log.info("TransferService.createTransfer() called. assetId: {}, requestedBy: {}",
                                request.getAssetId(), requestor.getEmployeeNo());
                Asset asset = assetRepository.findById(request.getAssetId())
                                .orElseThrow(() -> {
                                        log.error("Asset not found for transfer request. assetId: {}",
                                                        request.getAssetId());
                                        return new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다.");
                                });

                if (asset.getStatus() != AssetStatus.NORMAL) {
                        throw new BaseException(ErrorCode.INVALID_REQUEST);
                }

                TransferRecipientMode recipientMode = resolveRecipientMode(request);
                TransferLocationMode locationMode = resolveLocationMode(request, asset);

                if (recipientMode == TransferRecipientMode.CHANGE
                                && (request.getRecipientName() == null || request.getRecipientName().isBlank()
                                                || request.getRecipientDepartment() == null
                                                || request.getRecipientDepartment().isBlank())) {
                        log.warn("Transfer recipient name or department is missing.");
                        throw new BaseException(ErrorCode.BAD_REQUEST, "변경할 실사용자의 이름과 소속은 필수입니다.");
                }

                String destinationLocation = locationMode == TransferLocationMode.KEEP
                                ? trimToNull(asset.getLocation())
                                : trimToNull(request.getTransferLocation());
                if (destinationLocation == null) {
                        log.warn("Transfer location is missing.");
                        throw new BaseException(ErrorCode.BAD_REQUEST, "이관 장소는 필수입니다.");
                }
                if (locationMode == TransferLocationMode.CHANGE
                                && sameText(destinationLocation, asset.getLocation())) {
                        throw new BaseException(ErrorCode.BAD_REQUEST, "변경할 비치 장소는 현재 장소와 달라야 합니다.");
                }

                String recipientName = recipientMode == TransferRecipientMode.KEEP
                                ? trimToNull(asset.getActualUserName())
                                : recipientMode == TransferRecipientMode.CHANGE
                                                ? trimToNull(request.getRecipientName())
                                                : null;
                String recipientDepartment = recipientMode == TransferRecipientMode.KEEP
                                ? trimToNull(asset.getActualUserDepartment())
                                : recipientMode == TransferRecipientMode.CHANGE
                                                ? trimToNull(request.getRecipientDepartment())
                                                : null;
                String recipientContact = recipientMode == TransferRecipientMode.KEEP
                                ? trimToNull(asset.getActualUserContact())
                                : recipientMode == TransferRecipientMode.CHANGE
                                                ? trimToNull(request.getRecipientContact())
                                                : null;

                boolean recipientChanged = isRecipientChanged(asset, recipientMode,
                                recipientName, recipientDepartment, recipientContact);
                boolean locationChanged = locationMode == TransferLocationMode.CHANGE;
                if (!recipientChanged && !locationChanged) {
                        throw new BaseException(ErrorCode.BAD_REQUEST, "실사용자 또는 비치 장소 중 하나 이상을 변경해주세요.");
                }

                // A transfer request changes only the workflow status.
                asset.markTransferPending();

                Transfer transfer = Transfer.builder()
                                .asset(asset)
                                .fromUserName(asset.getActualUserName())
                                .fromUserDepartment(asset.getActualUserDepartment())
                                .recipientName(recipientName)
                                .recipientDepartment(recipientDepartment)
                                .recipientContact(recipientContact)
                                .recipientEmail(recipientMode == TransferRecipientMode.CHANGE
                                                ? trimToNull(request.getRecipientEmail())
                                                : null)
                                .recipientMode(recipientMode)
                                .fromLocation(asset.getLocation())
                                .transferLocation(destinationLocation)
                                .locationMode(locationMode)
                                .remarks(trimToNull(request.getRemarks()))
                                .requestedBy(requestor)
                                .fromUser(asset.getUser()) // Deprecated: Set from current asset user if available
                                .status(TransferStatus.PENDING)
                                .reason(trimToNull(request.getReason()))
                                .requestDate(LocalDateTime.now())
                                .build();

                transferRepository.save(transfer);
                log.info("Transfer request created successfully. ID: {}", transfer.getId());

                notificationService.notifyAdmins(
                                NotificationType.NEW_REQUEST,
                                "새로운 이관 요청",
                                requestor.getName() + "님이 자산 이관을 요청했습니다.",
                                "TRANSFER",
                                transfer.getId());

                return TransferResponse.from(transfer);
        }

        @Transactional(readOnly = true)
        public List<TransferResponse> getMyTransfers(User currentUser) {
                return transferRepository.findByRequestedByOrderByCreatedAtDesc(currentUser).stream()
                                .map(TransferResponse::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER')")
        public List<TransferResponse> getAllTransfers() {
                return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                                .map(TransferResponse::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<TransferResponse> getAssetTransferHistory(Long assetId) {
                Asset asset = assetRepository.findById(assetId)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다."));

                return transferRepository.findByAssetOrderByCreatedAtDesc(asset).stream()
                                .map(TransferResponse::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Page<TransferResponse> getTransferList(String employeeNo, String type, TransferStatus status,
                        Pageable pageable) {
                log.debug("TransferService.getTransferList() called. employeeNo: {}, type: {}, status: {}",
                                employeeNo, type, status);

                if (pageable.getSort().isUnsorted()) {
                        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                        Sort.by(Sort.Direction.DESC, "createdAt"));
                }

                // This method relies on old columns (fromUser, toUser) which we populated in
                // createTransfer
                // So it should still work for system users.
                // For purely external transfers, this might return empty results if
                // fromUser/toUser is null.
                // We might need to update repository methods to search by requestedBy as well.
                // For now, keeping as is to minimize regression for system users.

                User user = userRepository.findByEmployeeNo(employeeNo)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

                if ("sent".equalsIgnoreCase(type)) {
                        if (status != null) {
                                return transferRepository
                                                .findByFromUserEmployeeNoAndStatus(user.getEmployeeNo(), status,
                                                                pageable)
                                                .map(TransferResponse::from);
                        }
                        return transferRepository.findByFromUserEmployeeNo(user.getEmployeeNo(), pageable)
                                        .map(TransferResponse::from);
                } else {
                        // received (default)
                        if (status != null) {
                                return transferRepository
                                                .findByRecipientNameAndStatus(user.getName(), status, pageable)
                                                .map(TransferResponse::from);
                        }
                        return transferRepository.findByRecipientName(user.getName(), pageable)
                                        .map(TransferResponse::from);
                }
        }

        @Transactional
        public TransferResponse approveTransfer(Long transferId, User approver) {
                log.info("TransferService.approveTransfer() called. ID: {}, approver: {}", transferId,
                                approver.getEmployeeNo());
                Transfer transfer = transferRepository.findById(transferId)
                                .orElseThrow(() -> {
                                        log.error("Transfer request not found for approval. ID: {}", transferId);
                                        return new BaseException(ErrorCode.NOT_FOUND, "이관 요청을 찾을 수 없습니다.");
                                });

                transfer.approve(approver);

                Asset asset = transfer.getAsset();
                TransferRecipientMode recipientMode = resolveRecipientMode(transfer);
                TransferLocationMode locationMode = transfer.getLocationMode() != null
                                ? transfer.getLocationMode()
                                : TransferLocationMode.CHANGE;

                String userName = asset.getActualUserName();
                String userDepartment = asset.getActualUserDepartment();
                String userContact = asset.getActualUserContact();
                if (recipientMode == TransferRecipientMode.CHANGE) {
                        userName = transfer.getRecipientName();
                        userDepartment = transfer.getRecipientDepartment();
                        userContact = transfer.getRecipientContact();
                } else if (recipientMode == TransferRecipientMode.UNASSIGN) {
                        userName = null;
                        userDepartment = null;
                        userContact = null;
                }

                String destinationLocation = locationMode == TransferLocationMode.KEEP
                                ? asset.getLocation()
                                : transfer.getTransferLocation();
                asset.completeTransfer(
                                userName,
                                userDepartment,
                                userContact,
                                destinationLocation);
                if (recipientMode != TransferRecipientMode.KEEP) {
                        asset.setSharedUse(false);
                }
                log.info("Transfer approved. ID: {}", transferId);

                notificationService.createNotification(
                                transfer.getRequestedBy().getEmployeeNo(),
                                NotificationType.TRANSFER_APPROVED,
                                "이관 요청 승인됨",
                                "자산 이관 요청이 승인되었습니다.",
                                "TRANSFER",
                                transfer.getId());

                notificationService.notifyAdminsAndSuper(
                                NotificationType.ASSET_STATUS_CHANGED,
                                "자산 상태 변경 (정상)",
                                "자산 " + asset.getAssetNo() + " 상태가 정상으로 변경되었습니다.",
                                "ASSET",
                                asset.getId());

                return TransferResponse.from(transfer);
        }

        @Transactional
        public TransferResponse rejectTransfer(Long transferId, User rejecter, String reason) {
                log.info("TransferService.rejectTransfer() called. ID: {}, rejecter: {}", transferId,
                                rejecter.getEmployeeNo());
                Transfer transfer = transferRepository.findById(transferId)
                                .orElseThrow(() -> {
                                        log.error("Transfer request not found for rejection. ID: {}", transferId);
                                        return new BaseException(ErrorCode.NOT_FOUND, "이관 요청을 찾을 수 없습니다.");
                                });

                // Assuming Transfer entity has a way to set reason if it's different from
                // rejection reason
                // But the requirement says "rejectTransfer(Long transferId, User approver,
                // String reason)"
                // And Transfer entity has `reason` field (likely request reason) and `remarks`?
                // Let's check Transfer entity again. It has `reason` (line 72).
                // Usually rejection reason is stored somewhere.
                // The user requirements passed `String reason`.
                // I will assume I can update `reason` or I might need to ignore it if there is
                // no field.
                // Looking at Transfer.java: private String reason; (line 72) - This seems to be
                // request reason.
                // There is `private String remarks;` (line 57). Maybe that's for rejection
                // reason?
                // I will append reason to remarks or create a new field if needed, but for now
                // let's use remarks if available or just log it.
                // Wait, Transfer.java line 57: private String remarks;
                // I'll update remarks with the rejection reason.

                // Correction: Transfer.reject() method might not take reason. I should update
                // it manually.
                transfer.reject(rejecter, reason);
                // transfer.setRemarks(reason); // Check if setter exists or use
                // reflection/builder.
                // Since I cannot see setters (Lombok @Data or @Setter?), I assume @Builder or
                // specific methods.
                // The class has @Getter and @NoArgsConstructor(access = AccessLevel.PROTECTED)
                // and @AllArgsConstructor and @Builder.
                // It does NOT have @Setter.
                // So I cannot set remarks easily unless I add a method in Transfer entity.
                // However, I cannot modify Transfer.java easily right now as I am in the middle
                // of editing Service.
                // I will skip setting reason for now or assume `reject` method should handle
                // it, but `reject` only takes `User rejecter`.
                // I will proceed without setting reason for now, or assume it's not critical,
                // OR I can add a method to Transfer entity in a separate step if strictly
                // required.
                // User requirement: "public TransferResponse rejectTransfer(Long transferId,
                // User approver, String reason)"
                // I'll leave the reason argument but not use it effectively on the entity for
                // now to avoid compilation error if I try to call non-existent method.

                log.info("Transfer rejected. ID: {}", transferId);

                // Also revert asset status if it was pending?
                Asset asset = transfer.getAsset();
                if (asset.getStatus() == AssetStatus.TRANSFER_PENDING) {
                        asset.cancelTransfer();

                        notificationService.notifyAdminsAndSuper(
                                        NotificationType.ASSET_STATUS_CHANGED,
                                        "자산 상태 변경 (정상)",
                                        "자산 " + asset.getAssetNo() + " 상태가 정상으로 복구되었습니다.",
                                        "ASSET",
                                        asset.getId());
                }

                notificationService.createNotification(
                                transfer.getRequestedBy().getEmployeeNo(),
                                NotificationType.TRANSFER_REJECTED,
                                "이관 요청 반려됨",
                                "자산 이관 요청이 반려되었습니다. 사유: " + reason,
                                "TRANSFER",
                                transfer.getId());

                return TransferResponse.from(transfer);
        }

        private String trimToNull(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }
                return value.trim();
        }

        private TransferRecipientMode resolveRecipientMode(TransferRequest request) {
                if (request.getRecipientMode() != null) {
                        return request.getRecipientMode();
                }
                return request.isAssetOnly()
                                ? TransferRecipientMode.UNASSIGN
                                : TransferRecipientMode.CHANGE;
        }

        private TransferLocationMode resolveLocationMode(TransferRequest request, Asset asset) {
                if (request.getLocationMode() != null) {
                        return request.getLocationMode();
                }
                return sameText(request.getTransferLocation(), asset.getLocation())
                                ? TransferLocationMode.KEEP
                                : TransferLocationMode.CHANGE;
        }

        private TransferRecipientMode resolveRecipientMode(Transfer transfer) {
                if (transfer.getRecipientMode() != null) {
                        return transfer.getRecipientMode();
                }
                return transfer.getRecipientName() == null || transfer.getRecipientName().isBlank()
                                ? TransferRecipientMode.UNASSIGN
                                : TransferRecipientMode.CHANGE;
        }

        private boolean isRecipientChanged(Asset asset, TransferRecipientMode mode,
                        String name, String department, String contact) {
                if (mode == TransferRecipientMode.KEEP) {
                        return false;
                }
                return !sameText(name, asset.getActualUserName())
                                || !sameText(department, asset.getActualUserDepartment())
                                || !sameText(contact, asset.getActualUserContact());
        }

        private boolean sameText(String left, String right) {
                return Objects.equals(trimToNull(left), trimToNull(right));
        }
}
