package org.example.giic_asset.service;

import org.example.giic_asset.dto.loss.AssetLossRequestCreate;
import org.example.giic_asset.dto.loss.AssetLossResponse;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.AssetLossRequest;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.entity.enums.LossRequestStatus;
import org.example.giic_asset.repository.AssetLossRequestRepository;
import org.example.giic_asset.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetLossServiceTest {

    @Mock
    private AssetLossRequestRepository lossRequestRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AssetLossService assetLossService;

    private Asset asset;
    private User requester;
    private User manager;

    @BeforeEach
    void setUp() {
        asset = Asset.builder()
                .id(1L)
                .assetNo("2411309")
                .name("Monitor")
                .status(AssetStatus.NORMAL)
                .build();

        requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();

        manager = User.builder()
                .employeeNo("2012067")
                .name("Manager")
                .build();
    }

    @Test
    void requestLossMovesAssetToPendingWithoutFinalLossFields() {
        LocalDate lossDate = LocalDate.of(2026, 7, 10);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(lossRequestRepository.save(any(AssetLossRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssetLossResponse response = assetLossService.requestLoss(
                new AssetLossRequestCreate(1L, lossDate, "Lost during office relocation"), requester);

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.LOSS_PENDING);
        assertThat(asset.getLossDate()).isNull();
        assertThat(asset.getLossRequesterId()).isNull();
        assertThat(response.getLossDate()).isEqualTo(lossDate);
        assertThat(response.getRemarks()).isEqualTo("Lost during office relocation");
        assertThat(response.getRequesterName()).isEqualTo("Requester");
        assertThat(response.getStatus()).isEqualTo(LossRequestStatus.PENDING);
        verify(notificationService).notifyAdminsAndSuper(any(), any(), any(), any(), any());
    }

    @Test
    void approveLossFinalizesAssetWithRequestInformation() {
        LocalDate lossDate = LocalDate.of(2026, 7, 10);
        asset.requestLoss();
        AssetLossRequest request = AssetLossRequest.builder()
                .asset(asset)
                .requesterEmployeeNo(requester.getEmployeeNo())
                .requesterName(requester.getName())
                .lossDate(lossDate)
                .remarks("Lost during office relocation")
                .status(LossRequestStatus.PENDING)
                .build();
        when(lossRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        AssetLossResponse response = assetLossService.approveLoss(10L, manager);

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.LOST);
        assertThat(asset.getLossDate()).isEqualTo(lossDate);
        assertThat(asset.getLossRequesterId()).isEqualTo("2025009");
        assertThat(asset.getLossRequesterName()).isEqualTo("Requester");
        assertThat(asset.getLossRemarks()).isEqualTo("Lost during office relocation");
        assertThat(request.getStatus()).isEqualTo(LossRequestStatus.APPROVED);
        assertThat(response.getProcessorName()).isEqualTo("Manager");
        verify(notificationService).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectLossRestoresNormalStatusWithoutFinalLossFields() {
        asset.requestLoss();
        AssetLossRequest request = AssetLossRequest.builder()
                .asset(asset)
                .requesterEmployeeNo(requester.getEmployeeNo())
                .requesterName(requester.getName())
                .lossDate(LocalDate.of(2026, 7, 10))
                .remarks("Asset could not be found")
                .status(LossRequestStatus.PENDING)
                .build();
        when(lossRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        AssetLossResponse response = assetLossService.rejectLoss(10L, manager, "Asset found");

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
        assertThat(asset.getLossDate()).isNull();
        assertThat(asset.getLossRequesterId()).isNull();
        assertThat(request.getStatus()).isEqualTo(LossRequestStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Asset found");
        verify(notificationService).createNotification(any(), any(), any(), any(), any(), any());
    }
}
