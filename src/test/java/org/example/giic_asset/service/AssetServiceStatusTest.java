package org.example.giic_asset.service;

import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.dto.asset.AssetRequest;
import org.example.giic_asset.dto.asset.AssetUpdateRequest;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.AssetUsageChangeHistory;
import org.example.giic_asset.entity.enums.AssetStatus;
import org.example.giic_asset.repository.AssetImageRepository;
import org.example.giic_asset.repository.AssetLossRequestRepository;
import org.example.giic_asset.repository.AssetRepository;
import org.example.giic_asset.repository.AssetUsageChangeHistoryRepository;
import org.example.giic_asset.repository.DisposalRepository;
import org.example.giic_asset.repository.MaintenanceRepository;
import org.example.giic_asset.repository.NotificationRepository;
import org.example.giic_asset.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceStatusTest {

    @Mock private AssetRepository assetRepository;
    @Mock private AssetUsageChangeHistoryRepository assetUsageChangeHistoryRepository;
    @Mock private AssetImageRepository assetImageRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private DisposalRepository disposalRepository;
    @Mock private AssetLossRequestRepository assetLossRequestRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AssetCategoryService categoryService;
    @InjectMocks private AssetService assetService;

    @Test
    void unusedCreationRequiresLocation() {
        AssetRequest request = new AssetRequest();
        ReflectionTestUtils.setField(request, "status", AssetStatus.UNUSED);
        ReflectionTestUtils.setField(request, "location", "  ");

        assertThatThrownBy(() -> assetService.createAsset(request, null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("비치장소");
        verify(assetRepository, never()).save(any());
    }

    @Test
    void unusedCreationClearsPlaceholderUser() {
        AssetRequest request = new AssetRequest();
        ReflectionTestUtils.setField(request, "assetNo", "2502278");
        ReflectionTestUtils.setField(request, "name", "테스트 자산");
        ReflectionTestUtils.setField(request, "status", AssetStatus.UNUSED);
        ReflectionTestUtils.setField(request, "location", "보관실");
        ReflectionTestUtils.setField(request, "actualUserName", "미사용");
        when(assetRepository.save(any(Asset.class))).thenAnswer(call -> call.getArgument(0));

        assetService.createAsset(request, null);

        org.mockito.ArgumentCaptor<Asset> saved = org.mockito.ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AssetStatus.UNUSED);
        assertThat(saved.getValue().getLocation()).isEqualTo("보관실");
        assertThat(saved.getValue().getActualUserName()).isNull();
    }

    @Test
    void movingToUnusedClearsUserButKeepsLocation() {
        Asset asset = asset(AssetStatus.NORMAL, "홍길동");
        AssetUpdateRequest request = new AssetUpdateRequest();
        request.setStatus(AssetStatus.UNUSED);
        request.setLocation("기존 장소");
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assetService.updateAsset(1L, request);

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.UNUSED);
        assertThat(asset.getLocation()).isEqualTo("기존 장소");
        assertThat(asset.getActualUserName()).isNull();
        assertThat(asset.getActualUserDepartment()).isNull();
        assertThat(asset.getActualUserContact()).isNull();
    }

    @Test
    void unusedUpdateRequiresLocation() {
        Asset asset = asset(AssetStatus.NORMAL, null);
        AssetUpdateRequest request = new AssetUpdateRequest();
        request.setStatus(AssetStatus.UNUSED);
        request.setLocation(" ");
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.updateAsset(1L, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("비치장소");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
    }

    @Test
    void movingFromUnusedToNormalRequiresAndStoresUser() {
        Asset asset = asset(AssetStatus.UNUSED, null);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        AssetUpdateRequest request = new AssetUpdateRequest();
        request.setStatus(AssetStatus.NORMAL);
        request.setLocation("기존 장소");

        assertThatThrownBy(() -> assetService.updateAsset(1L, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("실사용자 이름");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.UNUSED);

        request.setActualUserName("김민수");
        request.setActualUserDepartment("연구실");
        assetService.updateAsset(1L, request);

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
        assertThat(asset.getActualUserName()).isEqualTo("김민수");
        assertThat(asset.getActualUserDepartment()).isEqualTo("연구실");
    }

    @Test
    void faultStatusCanBeSetAndReverted() {
        Asset asset = asset(AssetStatus.NORMAL, null);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        AssetUpdateRequest request = new AssetUpdateRequest();
        request.setStatus(AssetStatus.ABNORMAL);

        assetService.updateAsset(1L, request);
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.ABNORMAL);

        request.setStatus(AssetStatus.NORMAL);
        assetService.updateAsset(1L, request);
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.NORMAL);
    }

    @Test
    void sharedCreationClearsIndividualUserWithoutChangingStatus() {
        AssetRequest request = new AssetRequest();
        ReflectionTestUtils.setField(request, "assetNo", "2405947");
        ReflectionTestUtils.setField(request, "name", "공용 자산");
        ReflectionTestUtils.setField(request, "status", AssetStatus.NORMAL);
        ReflectionTestUtils.setField(request, "sharedUse", true);
        ReflectionTestUtils.setField(request, "actualUserName", "이전 사용자");
        when(assetRepository.save(any(Asset.class))).thenAnswer(call -> call.getArgument(0));

        assetService.createAsset(request, null);

        org.mockito.ArgumentCaptor<Asset> saved = org.mockito.ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(saved.capture());
        assertThat(saved.getValue().isSharedUse()).isTrue();
        assertThat(saved.getValue().getStatus()).isEqualTo(AssetStatus.NORMAL);
        assertThat(saved.getValue().getActualUserName()).isNull();
    }

    @Test
    void sharedUpdateClearsUserAndOmittedFlagDoesNotReverseIt() {
        Asset asset = asset(AssetStatus.NORMAL, "이전 사용자");
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        AssetUpdateRequest request = new AssetUpdateRequest();
        request.setSharedUse(true);

        assetService.updateAsset(1L, request);

        assertThat(asset.isSharedUse()).isTrue();
        assertThat(asset.getActualUserName()).isNull();
        assertThat(asset.getActualUserDepartment()).isNull();
        org.mockito.ArgumentCaptor<AssetUsageChangeHistory> history =
                org.mockito.ArgumentCaptor.forClass(AssetUsageChangeHistory.class);
        verify(assetUsageChangeHistoryRepository).save(history.capture());
        assertThat(history.getValue().getAssetNo()).isEqualTo("2502278");
        assertThat(history.getValue().getPreviousUserName()).isEqualTo("이전 사용자");
        assertThat(history.getValue().getPreviousUserDepartment()).isEqualTo("연구실");
        assertThat(history.getValue().getPreviousUserContact()).isEqualTo("010-0000-0000");

        request.setSharedUse(null);
        request.setActualUserName("오래된 화면에서 입력한 이름");
        assetService.updateAsset(1L, request);

        assertThat(asset.isSharedUse()).isTrue();
        assertThat(asset.getActualUserName()).isNull();
        verify(assetUsageChangeHistoryRepository, times(1)).save(any());
    }

    @Test
    void onlyNormalAssetsCanBeChangedToSharedUse() {
        Asset asset = asset(AssetStatus.ABNORMAL, "기존 사용자");
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        AssetUpdateRequest request = new AssetUpdateRequest();
        request.setSharedUse(true);

        assertThatThrownBy(() -> assetService.updateAsset(1L, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("정상 자산");
        assertThat(asset.getActualUserName()).isEqualTo("기존 사용자");
        verify(assetUsageChangeHistoryRepository, never()).save(any());
    }

    private Asset asset(AssetStatus status, String userName) {
        return Asset.builder()
                .id(1L)
                .assetNo("2502278")
                .name("테스트 자산")
                .location("기존 장소")
                .status(status)
                .actualUserName(userName)
                .actualUserDepartment(userName == null ? null : "연구실")
                .actualUserContact(userName == null ? null : "010-0000-0000")
                .build();
    }
}
