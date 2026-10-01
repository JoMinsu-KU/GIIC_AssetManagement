package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.asset.AssetCategoryResponse;
import org.example.giic_asset.dto.asset.AssetCategoryAdminResponse;
import org.example.giic_asset.dto.asset.AssetCategoryCreateRequest;
import org.example.giic_asset.entity.AssetCategory;
import org.example.giic_asset.repository.AssetCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetCategoryService {
    private final AssetCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<AssetCategoryResponse> getTree() {
        List<AssetCategory> categories = categoryRepository.findAll().stream()
                .filter(AssetCategory::isActive)
                .sorted(Comparator.comparingInt(AssetCategory::getSortOrder))
                .toList();

        return categories.stream()
                .filter(category -> category.getParent() == null)
                .map(major -> new AssetCategoryResponse(
                        major.getId(),
                        major.getName(),
                        categories.stream()
                                .filter(category -> category.getParent() != null
                                        && category.getParent().getId().equals(major.getId()))
                                .map(category -> new AssetCategoryResponse.Subcategory(
                                        category.getId(), category.getName()))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetCategory requireActiveSubcategory(Long categoryId) {
        AssetCategory category = categoryId == null ? null : categoryRepository.findById(categoryId).orElse(null);
        if (category == null || !category.isActive() || category.getParent() == null
                || !category.getParent().isActive()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "유효한 중분류를 선택해 주세요.");
        }
        return category;
    }

    @Transactional(readOnly = true)
    public List<AssetCategoryAdminResponse> getAllForAdmin() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparingInt(AssetCategory::getSortOrder))
                .map(AssetCategoryAdminResponse::from)
                .toList();
    }

    @Transactional
    public AssetCategoryAdminResponse create(AssetCategoryCreateRequest request) {
        String name = normalizedName(request.name());
        AssetCategory parent = null;
        if (request.parentId() != null) {
            parent = findCategory(request.parentId());
            if (parent.getParent() != null || !parent.isActive()) {
                throw new BaseException(ErrorCode.BAD_REQUEST, "활성 대분류를 선택해 주세요.");
            }
        }
        ensureUniqueName(name, parent, null);
        int nextOrder = categoryRepository.findAll().stream()
                .mapToInt(AssetCategory::getSortOrder).max().orElse(0) + 1;
        String code;
        do {
            code = "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 19);
        } while (categoryRepository.existsByCode(code));
        return AssetCategoryAdminResponse.from(categoryRepository.save(
                new AssetCategory(code, name, parent, nextOrder)));
    }

    @Transactional
    public AssetCategoryAdminResponse rename(Long id, String requestedName) {
        AssetCategory category = findCategory(id);
        String name = normalizedName(requestedName);
        ensureUniqueName(name, category.getParent(), id);
        category.rename(name);
        return AssetCategoryAdminResponse.from(category);
    }

    @Transactional
    public AssetCategoryAdminResponse setActive(Long id, boolean active) {
        AssetCategory category = findCategory(id);
        if (active && category.getParent() != null && !category.getParent().isActive()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "먼저 상위 대분류를 활성화해 주세요.");
        }
        category.setActive(active);
        return AssetCategoryAdminResponse.from(category);
    }

    private AssetCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "분류를 찾을 수 없습니다."));
    }

    private String normalizedName(String requestedName) {
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "분류 이름은 1~100자로 입력해 주세요.");
        }
        return name;
    }

    private void ensureUniqueName(String name, AssetCategory parent, Long ownId) {
        boolean duplicate = categoryRepository.findAll().stream()
                .anyMatch(category -> !Objects.equals(category.getId(), ownId)
                        && (category.getParent() == null ? parent == null
                                : parent != null && category.getParent().getId().equals(parent.getId()))
                        && category.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new BaseException(ErrorCode.DUPLICATE, "같은 단계에 이미 존재하는 분류 이름입니다.");
        }
    }
}
