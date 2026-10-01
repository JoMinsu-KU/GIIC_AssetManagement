package org.example.giic_asset.service;

import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.entity.AssetCategory;
import org.example.giic_asset.repository.AssetCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetCategoryServiceTest {
    @Mock private AssetCategoryRepository categoryRepository;
    @InjectMocks private AssetCategoryService categoryService;

    @Test
    void acceptsOnlyAnActiveChildOfAnActiveMajor() {
        AssetCategory major = new AssetCategory("M01", "컴퓨팅", null, 1);
        AssetCategory child = new AssetCategory("S01", "노트북", major, 2);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(child));

        assertThat(categoryService.requireActiveSubcategory(2L)).isSameAs(child);

        ReflectionTestUtils.setField(major, "active", false);
        assertThatThrownBy(() -> categoryService.requireActiveSubcategory(2L))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void rejectsMissingAndMajorCategoryIds() {
        AssetCategory major = new AssetCategory("M01", "컴퓨팅", null, 1);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(major));

        assertThatThrownBy(() -> categoryService.requireActiveSubcategory(null))
                .isInstanceOf(BaseException.class);
        assertThatThrownBy(() -> categoryService.requireActiveSubcategory(1L))
                .isInstanceOf(BaseException.class);
    }
}
