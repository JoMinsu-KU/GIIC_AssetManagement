package org.example.giic_asset.service;

import org.example.giic_asset.repository.NotificationRepository;
import org.example.giic_asset.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void deleteAllNotificationsUsesOnlyCurrentUserId() {
        when(notificationRepository.deleteAllByRecipientEmployeeNo("2025009")).thenReturn(4);

        int deletedCount = notificationService.deleteAllNotifications("2025009");

        assertEquals(4, deletedCount);
        verify(notificationRepository).deleteAllByRecipientEmployeeNo("2025009");
    }
}
