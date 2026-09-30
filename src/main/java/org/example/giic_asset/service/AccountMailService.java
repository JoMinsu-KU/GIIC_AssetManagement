package org.example.giic_asset.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.event.SignupApprovedEvent;
import org.example.giic_asset.event.SignupRejectedEvent;
import org.example.giic_asset.event.SignupRequestedEvent;
import org.example.giic_asset.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountMailService {

    private static final DateTimeFormatter MAIL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm", Locale.KOREAN);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserRepository userRepository;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.mail.public-url:http://localhost:3000}")
    private String publicUrl;

    public void sendSignupReceived(SignupRequestedEvent event) {
        if (!canSendTo(event.email())) {
            return;
        }

        Context context = applicantContext(
                event.employeeNo(), event.name(), event.department(), event.email(), event.requestedAt());
        sendHtml(
                event.email(),
                "[GIIC 자산관리] 회원가입 요청이 접수되었습니다",
                "mail/signup-received",
                context);
    }

    public void sendSignupRequestToSuperAdmins(SignupRequestedEvent event) {
        if (!enabled) {
            log.debug("Account email is disabled; skipping super administrator signup notification.");
            return;
        }

        Set<String> recipients = userRepository.findByRoleAndStatus(Role.SUPER, UserStatus.APPROVED).stream()
                .map(User::getEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

        if (recipients.isEmpty()) {
            log.warn("No approved super administrator email is configured; signup request email was skipped.");
            return;
        }

        for (String recipient : recipients) {
            Context context = applicantContext(
                    event.employeeNo(), event.name(), event.department(), event.email(), event.requestedAt());
            context.setVariable("reviewUrl", publicUrl("/users?status=PENDING"));
            sendHtml(
                    recipient,
                    "[GIIC 자산관리] 새로운 회원가입 승인 요청이 있습니다",
                    "mail/super-signup-request",
                    context);
        }
    }

    public void sendSignupApproved(SignupApprovedEvent event) {
        if (!canSendTo(event.email())) {
            return;
        }

        Context context = applicantContext(
                event.employeeNo(), event.name(), event.department(), event.email(), event.processedAt());
        context.setVariable("loginUrl", publicUrl("/login"));
        sendHtml(
                event.email(),
                "[GIIC 자산관리] 회원가입이 승인되었습니다",
                "mail/signup-approved",
                context);
    }

    public void sendSignupRejected(SignupRejectedEvent event) {
        if (!canSendTo(event.email())) {
            return;
        }

        Context context = applicantContext(
                event.employeeNo(), event.name(), event.department(), event.email(), event.processedAt());
        sendHtml(
                event.email(),
                "[GIIC 자산관리] 회원가입 요청이 반려되었습니다",
                "mail/signup-rejected",
                context);
    }

    private Context applicantContext(
            String employeeNo,
            String name,
            String department,
            String email,
            LocalDateTime occurredAt) {
        Context context = new Context(Locale.KOREAN);
        context.setVariable("employeeNo", display(employeeNo));
        context.setVariable("name", display(name));
        context.setVariable("department", display(department));
        context.setVariable("email", display(email));
        context.setVariable("formattedDate", formatDate(occurredAt));
        return context;
    }

    private void sendHtml(String recipient, String subject, String template, Context context) {
        if (!StringUtils.hasText(fromAddress)) {
            log.error("MAIL_FROM or MAIL_USERNAME must be configured when account email is enabled.");
            return;
        }

        try {
            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, "GIIC 자산관리시스템");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Account notification email sent. template={}", template);
        } catch (Exception e) {
            log.error("Failed to send account notification email. template={}", template, e);
        }
    }

    private boolean canSendTo(String recipient) {
        if (!enabled) {
            log.debug("Account email is disabled; skipping user notification.");
            return false;
        }
        if (!StringUtils.hasText(recipient)) {
            log.warn("User email is empty; account notification was skipped.");
            return false;
        }
        return true;
    }

    private String publicUrl(String path) {
        String baseUrl = StringUtils.hasText(publicUrl) ? publicUrl.trim() : "http://localhost:3000";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    private String formatDate(LocalDateTime dateTime) {
        return (dateTime != null ? dateTime : LocalDateTime.now()).format(MAIL_DATE_FORMAT);
    }

    private String display(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }
}
