package org.example.giic_asset.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.event.SignupRequestedEvent;
import org.example.giic_asset.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountMailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;
    @Mock
    private UserRepository userRepository;

    private AccountMailService accountMailService;

    @BeforeEach
    void setUp() {
        accountMailService = new AccountMailService(mailSender, templateEngine, userRepository);
        ReflectionTestUtils.setField(accountMailService, "enabled", true);
        ReflectionTestUtils.setField(accountMailService, "fromAddress", "mailer@example.com");
        ReflectionTestUtils.setField(accountMailService, "publicUrl", "http://203.253.164.48:3000/");
    }

    @Test
    void sendsSignupRequestOnlyToDistinctApprovedSuperAdministratorEmails() {
        User firstSuper = superAdministrator("super@example.com");
        User duplicateSuper = superAdministrator("SUPER@example.com");
        User missingEmailSuper = superAdministrator(" ");
        when(userRepository.findByRoleAndStatus(Role.SUPER, UserStatus.APPROVED))
                .thenReturn(List.of(firstSuper, duplicateSuper, missingEmailSuper));
        when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>mail</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage());

        accountMailService.sendSignupRequestToSuperAdmins(requestedEvent());

        verify(userRepository).findByRoleAndStatus(Role.SUPER, UserStatus.APPROVED);
        verify(templateEngine).process(eq("mail/super-signup-request"), any(Context.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void disabledMailDoesNotQueryRecipientsOrSend() {
        ReflectionTestUtils.setField(accountMailService, "enabled", false);

        accountMailService.sendSignupRequestToSuperAdmins(requestedEvent());

        verify(userRepository, never()).findByRoleAndStatus(any(Role.class), any(UserStatus.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private User superAdministrator(String email) {
        return User.builder()
                .employeeNo("2025009")
                .password("encoded")
                .name("Super Administrator")
                .email(email)
                .role(Role.SUPER)
                .status(UserStatus.APPROVED)
                .build();
    }

    private SignupRequestedEvent requestedEvent() {
        return new SignupRequestedEvent(
                "2026001",
                "Test User",
                "GIIC",
                "user@example.com",
                LocalDateTime.of(2026, 7, 14, 14, 30));
    }

    private MimeMessage mimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
