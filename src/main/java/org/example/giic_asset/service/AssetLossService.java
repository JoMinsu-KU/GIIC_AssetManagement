package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.loss.AssetLossRequestCreate;
import org.example.giic_asset.dto.loss.AssetLossResponse;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.AssetLossRequest;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.entity.enums.LossRequestStatus;
import org.example.giic_asset.entity.enums.NotificationType;
import org.example.giic_asset.repository.AssetLossRequestRepository;
import org.example.giic_asset.repository.AssetRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetLossService {

    private final AssetLossRequestRepository lossRequestRepository;
    private final AssetRepository assetRepository;
    private final NotificationService notificationService;

    @Transactional
    public AssetLossResponse requestLoss(AssetLossRequestCreate request, User requester) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다."));

        if (asset.getStatus() != AssetStatus.NORMAL) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "정상 상태의 자산만 손망 신청할 수 있습니다.");
        }

        asset.requestLoss();
        AssetLossRequest lossRequest = AssetLossRequest.builder()
                .asset(asset)
                .requesterEmployeeNo(requester.getEmployeeNo())
                .requesterName(requester.getName())
                .lossDate(request.getLossDate())
                .remarks(request.getRemarks().trim())
                .status(LossRequestStatus.PENDING)
                .build();
        AssetLossRequest saved = lossRequestRepository.save(lossRequest);

        notificationService.notifyAdminsAndSuper(
                NotificationType.NEW_REQUEST,
                "새로운 손망 신청",
                requester.getName() + "님이 자산 " + asset.getAssetNo() + "의 손망을 신청했습니다.",
                "LOSS",
                saved.getId());

        log.info("Asset loss request created. requestId={}, assetId={}", saved.getId(), asset.getId());
        return AssetLossResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AssetLossResponse> getLossRequests(
            String employeeNo,
            Collection<? extends GrantedAuthority> authorities) {
        boolean isManager = authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_SUPER"));

        List<AssetLossRequest> requests = isManager
                ? lossRequestRepository.findAllByOrderByCreatedAtDesc()
                : lossRequestRepository.findByRequesterEmployeeNoOrderByCreatedAtDesc(employeeNo);
        return requests.stream().map(AssetLossResponse::from).toList();
    }

    @Transactional
    public AssetLossResponse approveLoss(Long id, User approver) {
        AssetLossRequest request = findPendingRequest(id);
        Asset asset = request.getAsset();
        if (asset.getStatus() != AssetStatus.LOSS_PENDING) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "손망 승인 대기 상태가 아닙니다.");
        }

        request.approve(approver);
        asset.confirmLoss(
                request.getLossDate(),
                request.getRequesterEmployeeNo(),
                request.getRequesterName(),
                request.getRemarks());

        notificationService.createNotification(
                request.getRequesterEmployeeNo(),
                NotificationType.ASSET_STATUS_CHANGED,
                "손망 신청 승인",
                "자산 " + asset.getAssetNo() + "의 손망 신청이 승인되었습니다.",
                "ASSET",
                asset.getId());

        log.info("Asset loss request approved. requestId={}, assetId={}", id, asset.getId());
        return AssetLossResponse.from(request);
    }

    @Transactional
    public AssetLossResponse rejectLoss(Long id, User rejecter, String rejectionReason) {
        AssetLossRequest request = findPendingRequest(id);
        Asset asset = request.getAsset();
        if (asset.getStatus() != AssetStatus.LOSS_PENDING) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "손망 승인 대기 상태가 아닙니다.");
        }

        request.reject(rejecter, rejectionReason);
        asset.rejectLoss();

        notificationService.createNotification(
                request.getRequesterEmployeeNo(),
                NotificationType.ASSET_STATUS_CHANGED,
                "손망 신청 반려",
                "자산 " + asset.getAssetNo() + "의 손망 신청이 반려되었습니다.",
                "LOSS",
                request.getId());

        log.info("Asset loss request rejected. requestId={}, assetId={}", id, asset.getId());
        return AssetLossResponse.from(request);
    }

    private AssetLossRequest findPendingRequest(Long id) {
        AssetLossRequest request = lossRequestRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "손망 신청을 찾을 수 없습니다."));
        if (request.getStatus() != LossRequestStatus.PENDING) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "이미 처리된 손망 신청입니다.");
        }
        return request;
    }
}
