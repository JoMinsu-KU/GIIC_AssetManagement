package org.example.giic_asset.service;

import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.dto.asset.AssetCategoryCreateRequest;
import org.example.giic_asset.entity.AssetCategory;
import org.example.giic_asset.repository.AssetCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetCategoryAdminTest {
    @Mock private AssetCategoryRepository repository;
    @InjectMocks private AssetCategoryService service;

    @Test
    void createsMiddleCategoryUnderAnActiveMajor() {
        AssetCategory major = category(1L, "M01", "컴퓨팅", null);
        when(repository.findById(1L)).thenReturn(Optional.of(major));
        when(repository.findAll()).thenReturn(List.of(major));
        when(repository.save(any(AssetCategory.class))).thenAnswer(call -> {
            AssetCategory saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });

        var created = service.create(new AssetCategoryCreateRequest(" 서버 ", 1L));

        assertThat(created.name()).isEqualTo("서버");
        assertThat(created.parentId()).isEqualTo(1L);
        assertThat(created.active()).isTrue();
        assertThat(created.code()).startsWith("C");
    }

    @Test
    void rejectsDuplicateNameAndNestedCategory() {
        AssetCategory major = category(1L, "M01", "컴퓨팅", null);
        AssetCategory child = category(2L, "S01", "노트북", major);
        when(repository.findById(2L)).thenReturn(Optional.of(child));
        assertThatThrownBy(() -> service.create(new AssetCategoryCreateRequest("서버", 2L)))
                .isInstanceOf(BaseException.class);

        when(repository.findAll()).thenReturn(List.of(major));
        assertThatThrownBy(() -> service.create(new AssetCategoryCreateRequest(" 컴퓨팅 ", null)))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void disablingCategoryKeepsItsIdentity() {
        AssetCategory child = category(2L, "S01", "노트북", category(1L, "M01", "컴퓨팅", null));
        when(repository.findById(2L)).thenReturn(Optional.of(child));

        var updated = service.setActive(2L, false);

        assertThat(updated.id()).isEqualTo(2L);
        assertThat(updated.active()).isFalse();
        assertThat(updated.parentId()).isEqualTo(1L);
    }

    private AssetCategory category(Long id, String code, String name, AssetCategory parent) {
        AssetCategory category = new AssetCategory(code, name, parent, id.intValue());
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
