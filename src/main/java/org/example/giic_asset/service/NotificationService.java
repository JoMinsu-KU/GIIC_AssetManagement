package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.dto.notification.NotificationResponse;
import org.example.giic_asset.entity.Notification;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.NotificationType;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.repository.NotificationRepository;
import org.example.giic_asset.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(String recipientId, NotificationType type, String title, String message,
            String entityType, Long entityId) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + recipientId));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    public List<NotificationResponse> getNotifications(String userId) {
        return notificationRepository.findByRecipientEmployeeNoOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientEmployeeNoAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getRecipient().getEmployeeNo().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to notification");
        }

        notification.setRead(true);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> notifications = notificationRepository.findByRecipientEmployeeNoOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
    }

    @Transactional
    public int deleteAllNotifications(String userId) {
        return notificationRepository.deleteAllByRecipientEmployeeNo(userId);
    }

    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message, String entityType, Long entityId) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            createNotification(admin.getEmployeeNo(), type, title, message, entityType, entityId);
        }
    }

    @Transactional
    public void notifySuper(NotificationType type, String title, String message, String entityType, Long entityId) {
        List<User> supers = userRepository.findByRole(Role.SUPER);
        for (User user : supers) {
            createNotification(user.getEmployeeNo(), type, title, message, entityType, entityId);
        }
    }

    @Transactional
    public void notifyAdminsAndSuper(NotificationType type, String title, String message, String entityType,
            Long entityId) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        List<User> supers = userRepository.findByRole(Role.SUPER);

        for (User admin : admins) {
            createNotification(admin.getEmployeeNo(), type, title, message, entityType, entityId);
        }
        for (User sup : supers) {
            createNotification(sup.getEmployeeNo(), type, title, message, entityType, entityId);
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
