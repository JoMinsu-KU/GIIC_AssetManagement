package org.example.giic_asset.controller;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.dto.ApiResponse;
import org.example.giic_asset.dto.notification.NotificationResponse;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationResponse> notifications = notificationService.getNotifications(userDetails.getUsername());
        return ApiResponse.success(notifications);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ApiResponse.success(count);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(id, userDetails.getUsername());
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAllNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteAllNotifications(userDetails.getUsername());
        return ApiResponse.success("모든 알림이 삭제되었습니다.");
    }
}
