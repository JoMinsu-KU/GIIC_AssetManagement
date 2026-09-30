package org.example.giic_asset.service;

import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.AssetImage;
import org.example.giic_asset.repository.AssetImageRepository;
import org.example.giic_asset.repository.AssetLossRequestRepository;
import org.example.giic_asset.repository.AssetRepository;
import org.example.giic_asset.repository.DisposalRepository;
import org.example.giic_asset.repository.MaintenanceRepository;
import org.example.giic_asset.repository.NotificationRepository;
import org.example.giic_asset.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceImageTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetImageRepository assetImageRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private MaintenanceRepository maintenanceRepository;
    @Mock
    private DisposalRepository disposalRepository;
    @Mock
    private AssetLossRequestRepository assetLossRequestRepository;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AssetService assetService;

    @TempDir
    Path uploadDir;

    private Asset asset;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(assetService, "uploadDir", uploadDir.toString());
        asset = Asset.builder().id(1L).assetNo("2502196").name("Desktop").build();
    }

    @Test
    void firstAddedImageBecomesPrimary() {
        MockMultipartFile file = imageFile("asset.jpg");
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetImageRepository.findOrderedByAssetId(1L)).thenReturn(List.of());

        assetService.addImages(1L, List.of(file), null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssetImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).isPrimary()).isTrue();
        assertThat(uploadDir.toFile().listFiles()).hasSize(1);
    }

    @Test
    void selectedBatchImageBecomesPrimaryAndExistingPrimaryIsCleared() {
        AssetImage existing = image(11L, true, "/uploads/existing.jpg");
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetImageRepository.findOrderedByAssetId(1L)).thenReturn(new ArrayList<>(List.of(existing)));

        assetService.addImages(1L, List.of(imageFile("front.jpg"), imageFile("label.png")), 1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssetImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetImageRepository).saveAll(captor.capture());
        assertThat(existing.isPrimary()).isFalse();
        assertThat(captor.getValue()).extracting(AssetImage::isPrimary).containsExactly(false, true);
    }

    @Test
    void imageCountCannotExceedFive() {
        List<AssetImage> existing = List.of(
                image(1L, true, "/uploads/1.jpg"),
                image(2L, false, "/uploads/2.jpg"),
                image(3L, false, "/uploads/3.jpg"),
                image(4L, false, "/uploads/4.jpg"),
                image(5L, false, "/uploads/5.jpg"));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetImageRepository.findOrderedByAssetId(1L)).thenReturn(existing);

        assertThatThrownBy(() -> assetService.addImages(1L, List.of(imageFile("extra.jpg")), null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("최대 5장");
    }

    @Test
    void deletingPrimaryPromotesNextImage() {
        AssetImage primary = image(11L, true, "/uploads/primary.jpg");
        AssetImage next = image(12L, false, "/uploads/next.jpg");
        when(assetImageRepository.findByIdAndAssetId(11L, 1L)).thenReturn(Optional.of(primary));
        when(assetImageRepository.findOrderedByAssetId(1L)).thenReturn(new ArrayList<>(List.of(primary, next)));

        assetService.deleteImage(1L, 11L);

        verify(assetImageRepository).delete(primary);
        assertThat(next.isPrimary()).isTrue();
    }

    @Test
    void settingPrimaryClearsOtherImages() {
        AssetImage first = image(11L, true, "/uploads/first.jpg");
        AssetImage second = image(12L, false, "/uploads/second.jpg");
        when(assetImageRepository.findOrderedByAssetId(1L)).thenReturn(new ArrayList<>(List.of(first, second)));

        assetService.setPrimaryImage(1L, 12L);

        assertThat(first.isPrimary()).isFalse();
        assertThat(second.isPrimary()).isTrue();
    }

    private MockMultipartFile imageFile(String name) {
        String contentType = name.endsWith(".png") ? "image/png" : "image/jpeg";
        return new MockMultipartFile("files", name, contentType, "image-content".getBytes());
    }

    private AssetImage image(Long id, boolean primary, String imageUrl) {
        return AssetImage.builder()
                .id(id)
                .asset(asset)
                .imageUrl(imageUrl)
                .isPrimary(primary)
                .build();
    }
}
