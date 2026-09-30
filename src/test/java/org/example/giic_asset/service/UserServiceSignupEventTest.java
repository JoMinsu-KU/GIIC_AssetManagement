package org.example.giic_asset.service;

import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.event.SignupApprovedEvent;
import org.example.giic_asset.event.SignupRejectedEvent;
import org.example.giic_asset.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceSignupEventTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private User targetUser;
    private User superAdministrator;

    @BeforeEach
    void setUp() {
        targetUser = User.builder()
                .employeeNo("2026001")
                .password("encoded")
                .name("Test User")
                .department("GIIC")
                .email("user@example.com")
                .role(Role.USER)
                .status(UserStatus.PENDING)
                .build();
        superAdministrator = User.builder()
                .employeeNo("2025009")
                .password("encoded")
                .name("Super Administrator")
                .role(Role.SUPER)
                .status(UserStatus.APPROVED)
                .build();
        when(userRepository.findByEmployeeNo("2026001")).thenReturn(Optional.of(targetUser));
    }

    @Test
    void approvalPublishesApprovedMailEvent() {
        userService.approveUser("2026001", superAdministrator);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SignupApprovedEvent event = (SignupApprovedEvent) eventCaptor.getValue();
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(event.email()).isEqualTo("user@example.com");
        assertThat(event.processedAt()).isNotNull();
    }

    @Test
    void rejectionPublishesRejectedMailEvent() {
        userService.rejectUser("2026001", superAdministrator);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SignupRejectedEvent event = (SignupRejectedEvent) eventCaptor.getValue();
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.REJECTED);
        assertThat(event.email()).isEqualTo("user@example.com");
        assertThat(event.processedAt()).isNotNull();
    }
}
