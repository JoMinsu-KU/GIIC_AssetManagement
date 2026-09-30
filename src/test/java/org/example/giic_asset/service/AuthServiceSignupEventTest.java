package org.example.giic_asset.service;

import org.example.giic_asset.dto.user.SignupRequest;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.NotificationType;
import org.example.giic_asset.event.SignupRequestedEvent;
import org.example.giic_asset.repository.UserRepository;
import org.example.giic_asset.security.JwtTokenProvider;
import org.example.giic_asset.security.TokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignupEventTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    void signupNotifiesSuperAdministratorsAndPublishesMailEvent() {
        SignupRequest request = signupRequest();
        when(userRepository.existsByEmployeeNo("2026001")).thenReturn(false);
        when(passwordEncoder.encode("secure-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.signup(request);

        verify(notificationService).notifySuper(
                eq(NotificationType.NEW_REQUEST),
                eq("새로운 회원가입 요청"),
                any(String.class),
                eq("USER"),
                eq(null));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SignupRequestedEvent event = (SignupRequestedEvent) eventCaptor.getValue();
        assertThat(event.employeeNo()).isEqualTo("2026001");
        assertThat(event.name()).isEqualTo("Test User");
        assertThat(event.email()).isEqualTo("user@example.com");
        assertThat(event.requestedAt()).isNotNull();
    }

    private SignupRequest signupRequest() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "employeeNo", "2026001");
        ReflectionTestUtils.setField(request, "password", "secure-password");
        ReflectionTestUtils.setField(request, "name", "Test User");
        ReflectionTestUtils.setField(request, "department", "GIIC");
        ReflectionTestUtils.setField(request, "email", "user@example.com");
        return request;
    }
}
