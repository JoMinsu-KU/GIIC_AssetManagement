package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.dto.ApiResponse;
import org.example.giic_asset.dto.asset.AssetCategoryActiveRequest;
import org.example.giic_asset.dto.asset.AssetCategoryAdminResponse;
import org.example.giic_asset.dto.asset.AssetCategoryCreateRequest;
import org.example.giic_asset.dto.asset.AssetCategoryRenameRequest;
import org.example.giic_asset.dto.system.AccessDashboardResponse;
import org.example.giic_asset.service.AssetCategoryService;
import org.example.giic_asset.service.UserAccessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER')")
public class SystemSettingsController {
    private final UserAccessService userAccessService;
    private final AssetCategoryService categoryService;

    @GetMapping("/access")
    public ApiResponse<AccessDashboardResponse> getAccessDashboard() {
        return ApiResponse.success(userAccessService.getDashboard());
    }

    @GetMapping("/categories")
    public ApiResponse<List<AssetCategoryAdminResponse>> getCategories() {
        return ApiResponse.success(categoryService.getAllForAdmin());
    }

    @PostMapping("/categories")
    public ApiResponse<AssetCategoryAdminResponse> createCategory(
            @RequestBody @Valid AssetCategoryCreateRequest request) {
        return ApiResponse.success(categoryService.create(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<AssetCategoryAdminResponse> renameCategory(
            @PathVariable Long id, @RequestBody @Valid AssetCategoryRenameRequest request) {
        return ApiResponse.success(categoryService.rename(id, request.name()));
    }

    @PatchMapping("/categories/{id}/active")
    public ApiResponse<AssetCategoryAdminResponse> setCategoryActive(
            @PathVariable Long id, @RequestBody @Valid AssetCategoryActiveRequest request) {
        return ApiResponse.success(categoryService.setActive(id, request.active()));
    }
}
