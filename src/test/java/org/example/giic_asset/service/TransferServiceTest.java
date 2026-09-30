package org.example.giic_asset.service;

import org.example.giic_asset.dto.asset.AssetResponse;
import org.example.giic_asset.dto.transfer.TransferResponse;
import org.example.giic_asset.dto.transfer.TransferRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferService transferService;

    @Test
    void createTransferChangesOnlyAssetStatus() {
        Asset asset = assetWithManagementInformation();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        TransferRequest request = new TransferRequest();
        ReflectionTestUtils.setField(request, "assetId", asset.getId());
        ReflectionTestUtils.setField(request, "recipientName", "New User");
        ReflectionTestUtils.setField(request, "recipientDepartment", "New Department");
        ReflectionTestUtils.setField(request, "transferLocation", "New Location");
        ReflectionTestUtils.setField(request, "recipientMode", TransferRecipientMode.CHANGE);
        ReflectionTestUtils.setField(request, "locationMode", TransferLocationMode.CHANGE);

        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transferService.createTransfer(request, requester);

        assertManagementInformationIsPreserved(asset);
        assertThat(asset.getLocation()).isEqualTo("Current Location");
        assertThat(asset.getActualUserName()).isEqualTo("Current User");
        assertThat(asset.getActualUserDepartment()).isEqualTo("Current Department");
        assertThat(asset.getActualUserContact()).isEqualTo("010-0000-0000");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.TRANSFER_PENDING);

        AssetResponse pendingDetail = AssetResponse.from(asset, List.of());
        assertThat(pendingDetail.getLocation()).isEqualTo("Current Location");
        assertThat(pendingDetail.getActualUserName()).isEqualTo("Current User");
        assertThat(pendingDetail.getActualUserDepartment()).isEqualTo("Current Department");
        assertThat(pendingDetail.getActualUserContact()).isEqualTo("010-0000-0000");
        assertThat(pendingDetail.getManagingDept()).isEqualTo("GIIC");
        assertThat(pendingDetail.getPurchaseDate()).isEqualTo(LocalDate.of(2025, 2, 25));
        assertThat(pendingDetail.getValue()).isEqualTo(480000L);
    }

    @Test
    void createAssetOnlyTransferIgnoresRecipientFieldsUntilApproval() {
        Asset asset = assetWithManagementInformation();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        TransferRequest request = new TransferRequest();
        ReflectionTestUtils.setField(request, "assetId", asset.getId());
        ReflectionTestUtils.setField(request, "assetOnly", true);
        ReflectionTestUtils.setField(request, "recipientName", "Ignored User");
        ReflectionTestUtils.setField(request, "recipientDepartment", "Ignored Department");
        ReflectionTestUtils.setField(request, "recipientContact", "010-9999-9999");
        ReflectionTestUtils.setField(request, "recipientEmail", "ignored@example.com");
        ReflectionTestUtils.setField(request, "transferLocation", "New Location");

        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.createTransfer(request, requester);

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(transferCaptor.capture());
        Transfer savedTransfer = transferCaptor.getValue();

        assertThat(response.isAssetOnly()).isTrue();
        assertThat(savedTransfer.getRecipientName()).isNull();
        assertThat(savedTransfer.getRecipientDepartment()).isNull();
        assertThat(savedTransfer.getRecipientContact()).isNull();
        assertThat(savedTransfer.getRecipientEmail()).isNull();
        assertThat(asset.getLocation()).isEqualTo("Current Location");
        assertThat(asset.getActualUserName()).isEqualTo("Current User");
        assertThat(asset.getActualUserDepartment()).isEqualTo("Current Department");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.TRANSFER_PENDING);
    }

    @Test
    void approveTransferUpdatesOnlyDestinationAndActualUser() {
        Asset asset = assetWithManagementInformation();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        User approver = User.builder()
                .employeeNo("2012067")
                .name("Manager")
                .build();
        Transfer transfer = Transfer.builder()
                .id(10L)
                .asset(asset)
                .recipientName("New User")
                .recipientDepartment("New Department")
                .recipientContact("010-1234-5678")
                .recipientMode(TransferRecipientMode.CHANGE)
                .transferLocation("New Location")
                .locationMode(TransferLocationMode.CHANGE)
                .requestedBy(requester)
                .status(TransferStatus.PENDING)
                .build();

        asset.markTransferPending();
        when(transferRepository.findById(10L)).thenReturn(Optional.of(transfer));

        transferService.approveTransfer(10L, approver);

        assertManagementInformationIsPreserved(asset);
        assertThat(asset.getLocation()).isEqualTo("New Location");
        assertThat(asset.getActualUserName()).isEqualTo("New User");
        assertThat(asset.getActualUserDepartment()).isEqualTo("New Department");
        assertThat(asset.getActualUserContact()).isEqualTo("010-1234-5678");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
    }

    @Test
    void approveAssetOnlyTransferClearsActualUserAndUpdatesOnlyLocation() {
        Asset asset = assetWithManagementInformation();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        User approver = User.builder()
                .employeeNo("2012067")
                .name("Manager")
                .build();
        Transfer transfer = Transfer.builder()
                .id(11L)
                .asset(asset)
                .recipientName("")
                .recipientMode(TransferRecipientMode.UNASSIGN)
                .transferLocation("New Location")
                .locationMode(TransferLocationMode.CHANGE)
                .requestedBy(requester)
                .status(TransferStatus.PENDING)
                .build();

        asset.markTransferPending();
        when(transferRepository.findById(11L)).thenReturn(Optional.of(transfer));

        transferService.approveTransfer(11L, approver);

        assertManagementInformationIsPreserved(asset);
        assertThat(asset.getLocation()).isEqualTo("New Location");
        assertThat(asset.getActualUserName()).isNull();
        assertThat(asset.getActualUserDepartment()).isNull();
        assertThat(asset.getActualUserContact()).isNull();
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
    }

    @Test
    void approveTransferKeepsActualUserWhenOnlyLocationChanges() {
        Asset asset = assetWithManagementInformation();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        User approver = User.builder()
                .employeeNo("2012067")
                .name("Manager")
                .build();
        Transfer transfer = Transfer.builder()
                .id(12L)
                .asset(asset)
                .recipientMode(TransferRecipientMode.KEEP)
                .recipientName("Current User")
                .recipientDepartment("Current Department")
                .recipientContact("010-0000-0000")
                .fromLocation("Current Location")
                .transferLocation("New Location")
                .locationMode(TransferLocationMode.CHANGE)
                .requestedBy(requester)
                .status(TransferStatus.PENDING)
                .build();

        asset.markTransferPending();
        when(transferRepository.findById(12L)).thenReturn(Optional.of(transfer));

        transferService.approveTransfer(12L, approver);

        assertManagementInformationIsPreserved(asset);
        assertThat(asset.getLocation()).isEqualTo("New Location");
        assertThat(asset.getActualUserName()).isEqualTo("Current User");
        assertThat(asset.getActualUserDepartment()).isEqualTo("Current Department");
        assertThat(asset.getActualUserContact()).isEqualTo("010-0000-0000");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
    }

    @Test
    void approveTransferKeepsLocationWhenOnlyActualUserChanges() {
        Asset asset = assetWithManagementInformation();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        User approver = User.builder()
                .employeeNo("2012067")
                .name("Manager")
                .build();
        Transfer transfer = Transfer.builder()
                .id(13L)
                .asset(asset)
                .recipientMode(TransferRecipientMode.CHANGE)
                .recipientName("New User")
                .recipientDepartment("New Department")
                .recipientContact("010-1234-5678")
                .fromLocation("Current Location")
                .transferLocation("Current Location")
                .locationMode(TransferLocationMode.KEEP)
                .requestedBy(requester)
                .status(TransferStatus.PENDING)
                .build();

        asset.markTransferPending();
        when(transferRepository.findById(13L)).thenReturn(Optional.of(transfer));

        transferService.approveTransfer(13L, approver);

        assertManagementInformationIsPreserved(asset);
        assertThat(asset.getLocation()).isEqualTo("Current Location");
        assertThat(asset.getActualUserName()).isEqualTo("New User");
        assertThat(asset.getActualUserDepartment()).isEqualTo("New Department");
        assertThat(asset.getActualUserContact()).isEqualTo("010-1234-5678");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
    }

    @Test
    void transferKeepsSharedUseForLocationOnlyAndClearsItForNewRecipient() {
        Asset asset = assetWithManagementInformation();
        asset.setSharedUse(true);
        User requester = User.builder().employeeNo("2025009").name("Requester").build();
        User approver = User.builder().employeeNo("2012067").name("Manager").build();
        Transfer locationTransfer = Transfer.builder()
                .id(20L).asset(asset).requestedBy(requester)
                .recipientMode(TransferRecipientMode.KEEP)
                .locationMode(TransferLocationMode.CHANGE)
                .transferLocation("Shared Room")
                .status(TransferStatus.PENDING).build();
        Transfer userTransfer = Transfer.builder()
                .id(21L).asset(asset).requestedBy(requester)
                .recipientMode(TransferRecipientMode.CHANGE)
                .recipientName("New User")
                .locationMode(TransferLocationMode.KEEP)
                .status(TransferStatus.PENDING).build();
        when(transferRepository.findById(20L)).thenReturn(Optional.of(locationTransfer));
        when(transferRepository.findById(21L)).thenReturn(Optional.of(userTransfer));

        transferService.approveTransfer(20L, approver);
        assertThat(asset.isSharedUse()).isTrue();
        assertThat(asset.getActualUserName()).isNull();

        transferService.approveTransfer(21L, approver);
        assertThat(asset.isSharedUse()).isFalse();
        assertThat(asset.getActualUserName()).isEqualTo("New User");
    }

    @Test
    void createTransferRejectsWhenRecipientAndLocationAreBothKept() {
        Asset asset = assetWithManagementInformation();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        TransferRequest request = new TransferRequest();
        ReflectionTestUtils.setField(request, "assetId", asset.getId());
        ReflectionTestUtils.setField(request, "recipientMode", TransferRecipientMode.KEEP);
        ReflectionTestUtils.setField(request, "locationMode", TransferLocationMode.KEEP);
        ReflectionTestUtils.setField(request, "transferLocation", "Current Location");

        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> transferService.createTransfer(request, requester))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("하나 이상을 변경");

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
    }

    @Test
    void getAssetTransferHistoryReturnsRepositoryOrderAndAuditFields() {
        Asset asset = Asset.builder()
                .id(1L)
                .assetNo("2411309")
                .name("Monitor")
                .status(AssetStatus.NORMAL)
                .build();
        User requester = User.builder()
                .employeeNo("2025009")
                .name("Requester")
                .build();
        User processor = User.builder()
                .employeeNo("2012067")
                .name("Manager")
                .build();
        Transfer latest = Transfer.builder()
                .id(2L)
                .asset(asset)
                .fromUserName("Previous User")
                .fromUserDepartment("Previous Department")
                .recipientName("Current User")
                .recipientDepartment("Current Department")
                .requestedBy(requester)
                .processedBy(processor)
                .status(TransferStatus.APPROVED)
                .requestDate(LocalDateTime.of(2026, 7, 13, 9, 0))
                .processDate(LocalDateTime.of(2026, 7, 13, 10, 0))
                .build();
        Transfer older = Transfer.builder()
                .id(1L)
                .asset(asset)
                .recipientName("Previous User")
                .recipientDepartment("Previous Department")
                .requestedBy(requester)
                .status(TransferStatus.REJECTED)
                .rejectReason("Incorrect recipient")
                .requestDate(LocalDateTime.of(2026, 6, 1, 9, 0))
                .build();

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(transferRepository.findByAssetOrderByCreatedAtDesc(asset)).thenReturn(List.of(latest, older));

        List<TransferResponse> result = transferService.getAssetTransferHistory(1L);

        assertThat(result).extracting(TransferResponse::getId).containsExactly(2L, 1L);
        assertThat(result.get(0).getRecipientName()).isEqualTo("Current User");
        assertThat(result.get(0).getProcessedByName()).isEqualTo("Manager");
        assertThat(result.get(1).getRejectReason()).isEqualTo("Incorrect recipient");
        verify(transferRepository).findByAssetOrderByCreatedAtDesc(asset);
    }

    private Asset assetWithManagementInformation() {
        return Asset.builder()
                .id(1L)
                .assetNo("2411309")
                .name("Monitor")
                .model("32UN650K")
                .spec("714.3*485.9*226.7(mm)")
                .purchaseDate(LocalDate.of(2025, 2, 25))
                .value(480000L)
                .managingDept("GIIC")
                .location("Current Location")
                .actualUserName("Current User")
                .actualUserDepartment("Current Department")
                .actualUserContact("010-0000-0000")
                .status(AssetStatus.NORMAL)
                .build();
    }

    private void assertManagementInformationIsPreserved(Asset asset) {
        assertThat(asset.getName()).isEqualTo("Monitor");
        assertThat(asset.getModel()).isEqualTo("32UN650K");
        assertThat(asset.getSpec()).isEqualTo("714.3*485.9*226.7(mm)");
        assertThat(asset.getPurchaseDate()).isEqualTo(LocalDate.of(2025, 2, 25));
        assertThat(asset.getValue()).isEqualTo(480000L);
        assertThat(asset.getManagingDept()).isEqualTo("GIIC");
    }
}
