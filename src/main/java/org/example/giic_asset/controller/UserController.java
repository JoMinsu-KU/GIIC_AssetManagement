package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.dto.ApiResponse;
import org.example.giic_asset.common.dto.PageResponse;
import org.example.giic_asset.dto.user.UserResponse;
import org.example.giic_asset.dto.user.UserActionRequest;
import org.example.giic_asset.dto.user.UserRoleUpdateRequest;
import org.example.giic_asset.dto.user.UserStatusUpdateRequest;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getUserList(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== GET /api/users ==========");
        log.info("Request params: role={}, status={}, keyword={}, page={}, size={}",
                role, status, keyword, pageable.getPageNumber(), pageable.getPageSize());

        Role currentUserRole = userDetails.getUser().getRole();

        try {
            Page<UserResponse> result = userService.getUserList(pageable, role, status, keyword, currentUserRole);
            log.info("Response: total_elements={}, data_count={}",
                    result.getTotalElements(), result.getContent().size());
            return ApiResponse.success(PageResponse.of(result));
        } catch (Exception e) {
            log.error("Failed to fetch user list: ", e);
            throw e;
        }
    }

    @PutMapping("/{employeeNo}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable String employeeNo,
            @RequestBody UserStatusUpdateRequest request) {
        log.info("========== PUT /api/users/{}/status ==========", employeeNo);
        log.info("Request status: {}, role: {}", request.getStatus(), request.getRole());
        try {
            userService.updateUserStatus(employeeNo, request);
            log.info("User status updated successfully");
            return ApiResponse.success("사용자 상태가 수정되었습니다.");
        } catch (Exception e) {
            log.error("Failed to update user status: ", e);
            throw e;
        }
    }

    @PutMapping("/{employeeNo}/approve")
    public ApiResponse<Void> approveUser(
            @PathVariable String employeeNo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/users/{}/approve ==========", employeeNo);
        userService.approveUser(employeeNo, userDetails.getUser());
        return ApiResponse.success("사용자가 승인되었습니다.");
    }

    @PutMapping("/{employeeNo}/reject")
    public ApiResponse<Void> rejectUser(
            @PathVariable String employeeNo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/users/{}/reject ==========", employeeNo);
        userService.rejectUser(employeeNo, userDetails.getUser());
        return ApiResponse.success("사용자가 거절되었습니다.");
    }

    @PutMapping("/reject")
    public ApiResponse<Void> rejectUserByRequest(
            @RequestBody @Valid UserActionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/users/reject ==========");
        userService.rejectUser(request.getEmployeeNo(), userDetails.getUser());
        return ApiResponse.success("사용자가 거절되었습니다.");
    }

    @PutMapping("/{employeeNo}/role")
    public ApiResponse<Void> updateUserRole(
            @PathVariable String employeeNo,
            @RequestBody UserRoleUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/users/{}/role ==========", employeeNo);
        userService.updateUserRole(employeeNo, request.getRole(), userDetails.getUser());
        return ApiResponse.success("사용자 권한이 변경되었습니다.");
    }

    @PutMapping("/{employeeNo}/deactivate")
    public ApiResponse<Void> deactivateUser(
            @PathVariable String employeeNo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("========== PUT /api/users/{}/deactivate ==========", employeeNo);
        userService.deactivateUser(employeeNo, userDetails.getUser());
        return ApiResponse.success("사용자가 비활성화되었습니다.");
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("========== GET /api/users/me ==========");
        log.info("User: {}", userDetails.getUsername());
        try {
            UserResponse response = userService.getMyInfo(userDetails.getUsername());
            log.info("My info retrieved successfully");
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Failed to get my info: ", e);
            throw e;
        }
    }
}
