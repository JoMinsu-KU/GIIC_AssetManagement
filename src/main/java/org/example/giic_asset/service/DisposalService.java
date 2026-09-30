package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.disposal.DisposalRequest;
import org.example.giic_asset.dto.disposal.DisposalResponse;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.Disposal;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.entity.enums.DisposalStatus;
import org.example.giic_asset.repository.AssetRepository;
import org.example.giic_asset.repository.DisposalRepository;
import org.example.giic_asset.entity.enums.NotificationType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisposalService {

        private final DisposalRepository disposalRepository;
        private final AssetRepository assetRepository;
        private final NotificationService notificationService;

        @Transactional
        public DisposalResponse requestDisposal(DisposalRequest request, User requester) {
                log.info("DisposalService.requestDisposal() called. assetId: {}, requester: {}",
                                request.getAssetId(), requester.getEmployeeNo());

                Asset asset = assetRepository.findById(request.getAssetId())
                                .orElseThrow(() -> {
                                        log.error("Asset not found for disposal request. assetId: {}",
                                                        request.getAssetId());
                                        return new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다.");
                                });

                if (asset.getStatus() != AssetStatus.NORMAL) {
                        throw new BaseException(ErrorCode.INVALID_REQUEST);
                }

                // Update asset status to DISPOSAL_PENDING
                asset.setStatus(AssetStatus.DISPOSAL_PENDING);
                assetRepository.save(asset);

                Disposal disposal = request.toEntity(asset, requester);
                disposalRepository.save(disposal);
                log.info("Disposal request created successfully. ID: {}", disposal.getId());

                notificationService.notifyAdmins(
                                NotificationType.NEW_REQUEST,
                                "새로운 폐기 요청",
                                requester.getName() + "님이 폐기를 요청했습니다.",
                                "DISPOSAL",
                                disposal.getId());

                return DisposalResponse.from(disposal);
        }

        @Transactional(readOnly = true)
        public List<DisposalResponse> getDisposalList(String employeeNo,
                        Collection<? extends GrantedAuthority> authorities) {
                boolean isAdmin = authorities.stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                                                || a.getAuthority().equals("ROLE_SUPER"));

                log.info("DisposalService.getDisposalList() called. employeeNo: {}, isAdmin: {}", employeeNo, isAdmin);

                if (isAdmin) {
                        return disposalRepository.findAllByOrderByCreatedAtDesc().stream()
                                        .map(DisposalResponse::from)
                                        .collect(Collectors.toList());
                } else {
                        return disposalRepository.findByRequesterEmployeeNoOrderByCreatedAtDesc(employeeNo).stream()
                                        .map(DisposalResponse::from)
                                        .collect(Collectors.toList());
                }
        }

        @Transactional
        public DisposalResponse approveDisposal(Long id, User approver) {
                log.info("DisposalService.approveDisposal() called. ID: {}, approver: {}", id,
                                approver.getEmployeeNo());

                Disposal disposal = disposalRepository.findById(id)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "폐기 요청을 찾을 수 없습니다."));

                disposal.approve(approver);

                // Update asset status to DISPOSED
                Asset asset = disposal.getAsset();
                asset.confirmDisposal(disposal.getCreatedAt(), disposal.getReason(), disposal.getRequesterEmployeeNo());

                assetRepository.save(asset); // Ensure asset is saved, though transaction might handle it

                log.info("Disposal approved. ID: {}", id);

                notificationService.createNotification(
                                disposal.getRequesterEmployeeNo(),
                                NotificationType.DISPOSAL_APPROVED,
                                "폐기 요청 승인됨",
                                "폐기 요청이 승인되었습니다.",
                                "DISPOSAL",
                                disposal.getId());

                notificationService.notifyAdminsAndSuper(
                                NotificationType.ASSET_STATUS_CHANGED,
                                "자산 상태 변경 (폐기)",
                                "자산 " + asset.getAssetNo() + " 상태가 폐기로 변경되었습니다.",
                                "ASSET",
                                asset.getId());

                return DisposalResponse.from(disposal);
        }

        @Transactional
        public DisposalResponse rejectDisposal(Long id, User rejecter, String reason) {
                log.info("DisposalService.rejectDisposal() called. ID: {}, rejecter: {}", id, rejecter.getEmployeeNo());

                Disposal disposal = disposalRepository.findById(id)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "폐기 요청을 찾을 수 없습니다."));

                disposal.reject(rejecter, reason);

                // Update asset status to NORMAL (revert)
                Asset asset = disposal.getAsset();
                asset.rejectDisposal(reason);

                assetRepository.save(asset);

                log.info("Disposal rejected. ID: {}", id);

                notificationService.createNotification(
                                disposal.getRequesterEmployeeNo(),
                                NotificationType.DISPOSAL_REJECTED,
                                "폐기 요청 반려됨",
                                "폐기 요청이 반려되었습니다. 사유: " + reason,
                                "DISPOSAL",
                                disposal.getId());

                notificationService.notifyAdminsAndSuper(
                                NotificationType.ASSET_STATUS_CHANGED,
                                "자산 상태 변경 (정상)",
                                "자산 " + asset.getAssetNo() + " 상태가 정상으로 복구되었습니다.",
                                "ASSET",
                                asset.getId());

                return DisposalResponse.from(disposal);
        }
}
