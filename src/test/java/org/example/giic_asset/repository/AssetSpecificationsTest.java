package org.example.giic_asset.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.giic_asset.dto.asset.AssetSearchCriteria;
import org.example.giic_asset.entity.Asset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetSpecificationsTest {

    @Mock private Root<Asset> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path<Boolean> sharedUsePath;
    @Mock private Predicate basePredicate;
    @Mock private Predicate sharedPredicate;

    @ParameterizedTest
    @ValueSource(strings = {"공용", " 공용 "})
    void sharedKeywordFindsOnlySharedAssets(String keyword) {
        when(cb.conjunction()).thenReturn(basePredicate);
        when(root.<Boolean>get("sharedUse")).thenReturn(sharedUsePath);
        when(cb.isTrue(sharedUsePath)).thenReturn(sharedPredicate);
        when(cb.and(basePredicate, sharedPredicate)).thenReturn(sharedPredicate);

        Predicate result = AssetSpecifications.withCriteria(
                AssetSearchCriteria.builder().keyword(keyword).build())
                .toPredicate(root, query, cb);

        assertThat(result).isSameAs(sharedPredicate);
        verify(root).get("sharedUse");
        verify(cb).isTrue(sharedUsePath);
    }
}
