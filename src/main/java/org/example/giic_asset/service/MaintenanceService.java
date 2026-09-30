package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.maintenance.MaintenanceRequest;
import org.example.giic_asset.dto.maintenance.MaintenanceResponse;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.Maintenance;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.entity.enums.MaintenanceStatus;
import org.example.giic_asset.repository.AssetRepository;
import org.example.giic_asset.repository.MaintenanceRepository;
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
public class MaintenanceService {

        private final MaintenanceRepository maintenanceRepository;
        private final AssetRepository assetRepository;
        private final NotificationService notificationService;

        @Transactional
        public MaintenanceResponse requestMaintenance(MaintenanceRequest request, User requester) {
                log.info("MaintenanceService.requestMaintenance() called. assetId: {}, requester: {}",
                                request.getAssetId(), requester.getEmployeeNo());

                Asset asset = assetRepository.findById(request.getAssetId())
                                .orElseThrow(() -> {
                                        log.error("Asset not found for maintenance request. assetId: {}",
                                                        request.getAssetId());
                                        return new BaseException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다.");
                                });

                if (asset.getStatus() != AssetStatus.NORMAL) {
                        throw new BaseException(ErrorCode.INVALID_REQUEST);
                }

                // Update asset status to REPAIRING
                asset.setStatus(AssetStatus.REPAIRING);
                assetRepository.save(asset);

                Maintenance maintenance = request.toEntity(asset, requester);
                maintenanceRepository.save(maintenance);
                log.info("Maintenance request created successfully. ID: {}", maintenance.getId());

                notificationService.notifySuper(
                                NotificationType.NEW_REQUEST,
                                "새로운 유지보수 요청",
                                requester.getName() + "님이 유지보수를 요청했습니다.",
                                "MAINTENANCE",
                                maintenance.getId());

                return MaintenanceResponse.from(maintenance);
        }

        @Transactional(readOnly = true)
        public List<MaintenanceResponse> getMaintenanceList(String employeeNo,
                        Collection<? extends GrantedAuthority> authorities) {
                boolean isAdmin = authorities.stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                                                || a.getAuthority().equals("ROLE_SUPER"));

                log.info("MaintenanceService.getMaintenanceList() called. employeeNo: {}, isAdmin: {}", employeeNo,
                                isAdmin);

                if (isAdmin) {
                        return maintenanceRepository.findAllByOrderByCreatedAtDesc().stream()
                                        .map(MaintenanceResponse::from)
                                        .collect(Collectors.toList());
                } else {
                        return maintenanceRepository.findByRequesterEmployeeNoOrderByCreatedAtDesc(employeeNo).stream()
                                        .map(MaintenanceResponse::from)
                                        .collect(Collectors.toList());
                }
        }

        @Transactional
        public MaintenanceResponse approveMaintenance(Long id, User approver) {
                log.info("MaintenanceService.approveMaintenance() called. ID: {}, approver: {}", id,
                                approver.getEmployeeNo());

                Maintenance maintenance = maintenanceRepository.findById(id)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "유지보수 요청을 찾을 수 없습니다."));

                maintenance.approve(approver);

                // Update asset status to REPAIRING
                Asset asset = maintenance.getAsset();
                asset.setStatus(AssetStatus.REPAIRING);
                assetRepository.save(asset);

                log.info("Maintenance approved. ID: {}", id);

                notificationService.createNotification(
                                maintenance.getRequester().getEmployeeNo(),
                                NotificationType.MAINTENANCE_APPROVED,
                                "유지보수 요청 승인됨",
                                "유지보수 요청이 승인되었습니다.",
                                "MAINTENANCE",
                                maintenance.getId());

                notificationService.notifyAdminsAndSuper(
                                NotificationType.ASSET_STATUS_CHANGED,
                                "자산 상태 변경 (수리중)",
                                "자산 " + asset.getAssetNo() + " 상태가 수리중으로 변경되었습니다.",
                                "ASSET",
                                asset.getId());

                return MaintenanceResponse.from(maintenanceRepository.save(maintenance));
        }

        @Transactional
        public MaintenanceResponse completeMaintenance(Long id, User completer) {
                log.info("MaintenanceService.completeMaintenance() called. ID: {}, completer: {}", id,
                                completer.getEmployeeNo());

                Maintenance maintenance = maintenanceRepository.findById(id)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "유지보수 요청을 찾을 수 없습니다."));

                maintenance.complete(completer);

                // Revert asset status to NORMAL
                Asset asset = maintenance.getAsset();
                asset.setStatus(AssetStatus.NORMAL);
                assetRepository.save(asset);

                log.info("Maintenance completed. ID: {}", id);

                notificationService.createNotification(
                                maintenance.getRequester().getEmployeeNo(),
                                NotificationType.MAINTENANCE_COMPLETED,
                                "유지보수 완료됨",
                                "유지보수 작업이 완료되었습니다.",
                                "MAINTENANCE",
                                maintenance.getId());

                notificationService.notifyAdminsAndSuper(
                                NotificationType.ASSET_STATUS_CHANGED,
                                "자산 상태 변경 (정상)",
                                "자산 " + asset.getAssetNo() + " 상태가 정상으로 변경되었습니다.",
                                "ASSET",
                                asset.getId());

                return MaintenanceResponse.from(maintenance);
        }

        @Transactional
        public MaintenanceResponse rejectMaintenance(Long id, User rejecter, String reason) {
                log.info("MaintenanceService.rejectMaintenance() called. ID: {}, rejecter: {}", id,
                                rejecter.getEmployeeNo());

                Maintenance maintenance = maintenanceRepository.findById(id)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "유지보수 요청을 찾을 수 없습니다."));

                maintenance.reject(rejecter, reason);

                // Revert asset status to NORMAL
                Asset asset = maintenance.getAsset();
                // Only revert if it was REPAIRING (which it should be if it came from
                // requestMaintenance)
                if (asset.getStatus() == AssetStatus.REPAIRING) {
                        asset.setStatus(AssetStatus.NORMAL);
                        assetRepository.save(asset);
                }

                log.info("Maintenance rejected. ID: {}", id);

                notificationService.createNotification(
                                maintenance.getRequester().getEmployeeNo(),
                                NotificationType.MAINTENANCE_REJECTED,
                                "유지보수 요청 반려됨",
                                "유지보수 요청이 반려되었습니다. 사유: " + reason,
                                "MAINTENANCE",
                                maintenance.getId());

                if (asset.getStatus() == AssetStatus.NORMAL) {
                        notificationService.notifyAdminsAndSuper(
                                        NotificationType.ASSET_STATUS_CHANGED,
                                        "자산 상태 변경 (정상)",
                                        "자산 " + asset.getAssetNo() + " 상태가 정상으로 복구되었습니다.",
                                        "ASSET",
                                        asset.getId());
                }

                return MaintenanceResponse.from(maintenance);
        }

        // Optional: Update maintenance details if needed (e.g. cost, description
        // correction)
        @Transactional
        public void updateMaintenance(Long maintenanceId, MaintenanceRequest request) {
                log.info("MaintenanceService.updateMaintenance() called. ID: {}", maintenanceId);
                Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "유지보수 기록을 찾을 수 없습니다."));

                maintenance.update(
                                request.getMaintenanceDate(),
                                request.getMaintenanceType(),
                                request.getDescription(),
                                request.getCost(),
                                request.getPerformedBy());
                log.info("Maintenance record updated. ID: {}", maintenanceId);
        }

        @Transactional
        public void deleteMaintenance(Long maintenanceId) {
                log.info("MaintenanceService.deleteMaintenance() called. ID: {}", maintenanceId);
                if (!maintenanceRepository.existsById(maintenanceId)) {
                        throw new BaseException(ErrorCode.NOT_FOUND, "유지보수 기록을 찾을 수 없습니다.");
                }
                maintenanceRepository.deleteById(maintenanceId);
                log.info("Maintenance record deleted. ID: {}", maintenanceId);
        }
}
