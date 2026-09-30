package org.example.giic_asset.service;

import org.example.giic_asset.dto.asset.AssetSearchCriteria;
import org.example.giic_asset.entity.Asset;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServicePaginationTest {

    @Mock private AssetRepository assetRepository;
    @Mock private AssetUsageChangeHistoryRepository assetUsageChangeHistoryRepository;
    @Mock private AssetImageRepository assetImageRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private DisposalRepository disposalRepository;
    @Mock private AssetLossRequestRepository assetLossRequestRepository;
    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private AssetService assetService;

    @Test
    void defaultSortUsesUniqueIdToBreakRegistrationDateTies() {
        Pageable queried = queryPageable(PageRequest.of(1, 10));

        assertThat(queried.getPageNumber()).isEqualTo(1);
        assertThat(queried.getSort().toList()).containsExactly(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    @Test
    void requestedSortKeepsItsPriorityAndGetsStableTieBreaker() {
        Pageable queried = queryPageable(PageRequest.of(0, 10, Sort.by("name").ascending()));

        assertThat(queried.getSort().toList()).containsExactly(
                Sort.Order.asc("name"), Sort.Order.desc("id"));
    }

    @Test
    void explicitIdSortIsNotDuplicated() {
        Pageable queried = queryPageable(PageRequest.of(0, 10, Sort.by("id").ascending()));

        assertThat(queried.getSort().toList()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void filteredPagesUseTheSameStableOrdering() {
        AssetSearchCriteria criteria = AssetSearchCriteria.builder()
                .status(AssetStatus.NORMAL)
                .location("PI-LAM")
                .build();
        Pageable queried = queryPageable(PageRequest.of(2, 10), criteria);

        assertThat(queried.getPageNumber()).isEqualTo(2);
        assertThat(queried.getSort().toList()).containsExactly(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    private Pageable queryPageable(Pageable requested) {
        return queryPageable(requested, AssetSearchCriteria.empty());
    }

    private Pageable queryPageable(Pageable requested, AssetSearchCriteria criteria) {
        when(assetRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Asset>>any(), any(Pageable.class)))
                .thenAnswer(call -> Page.<Asset>empty(call.getArgument(1)));

        assetService.getAssetList(requested, criteria);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(assetRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Asset>>any(), captor.capture());
        return captor.getValue();
    }
}
