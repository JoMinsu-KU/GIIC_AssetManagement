package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.auth.LoginRequest;
import org.example.giic_asset.dto.auth.TokenResponse;
import org.example.giic_asset.dto.user.SignupRequest;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.entity.enums.NotificationType;
import org.example.giic_asset.event.SignupRequestedEvent;
import org.example.giic_asset.repository.UserRepository;
import org.example.giic_asset.security.JwtTokenProvider;
import org.example.giic_asset.security.TokenStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        log.info("AuthService.login() called for employeeNo: {}", request.getEmployeeNo());

        User user = userRepository.findByEmployeeNo(request.getEmployeeNo())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found for employeeNo: {}", request.getEmployeeNo());
                    return new BaseException(ErrorCode.UNAUTHORIZED, "User not found.");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Password mismatch for employeeNo: {}", request.getEmployeeNo());
            throw new BaseException(ErrorCode.UNAUTHORIZED, "Invalid password.");
        }

        validateApprovedUser(user, request.getEmployeeNo());

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmployeeNo(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmployeeNo());
        tokenStore.storeRefreshToken(refreshToken, jwtTokenProvider.getExpirationMillis(refreshToken));

        log.info("Login successful for employeeNo: {}, Role: {}", request.getEmployeeNo(), user.getRole());
        return buildTokenResponse(user, accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("Refresh token validation failed");
            throw new BaseException(ErrorCode.UNAUTHORIZED, "Invalid refresh token.");
        }

        String employeeNo = jwtTokenProvider.getEmployeeNo(refreshToken);
        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "User not found."));

        validateApprovedUser(user, employeeNo);

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmployeeNo(), user.getRole());
        log.info("Token refreshed for employeeNo: {}", employeeNo);
        return buildTokenResponse(user, accessToken, refreshToken);
    }

    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BaseException(ErrorCode.UNAUTHORIZED, "Access token is required.");
        }

        if (jwtTokenProvider.validateAccessToken(accessToken)) {
            tokenStore.revokeAccessToken(accessToken, jwtTokenProvider.getExpirationMillis(accessToken));
        }
        log.info("Logout processed");
    }

    @Transactional
    public void signup(SignupRequest request) {
        log.info("AuthService.signup() called for employeeNo: {}", request.getEmployeeNo());

        if (userRepository.existsByEmployeeNo(request.getEmployeeNo())) {
            log.warn("Signup failed: Duplicate employeeNo: {}", request.getEmployeeNo());
            throw new BaseException(ErrorCode.DUPLICATE, "Employee number already exists.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User savedUser = userRepository.save(request.toEntity(encodedPassword));
        notificationService.notifySuper(
                NotificationType.NEW_REQUEST,
                "새로운 회원가입 요청",
                savedUser.getName() + "님의 회원가입 승인 요청이 도착했습니다.",
                "USER",
                null);
        eventPublisher.publishEvent(new SignupRequestedEvent(
                savedUser.getEmployeeNo(),
                savedUser.getName(),
                savedUser.getDepartment(),
                savedUser.getEmail(),
                savedUser.getCreatedAt() != null ? savedUser.getCreatedAt() : LocalDateTime.now()));
        log.info("User created: employeeNo={}, name={}, department={}",
                savedUser.getEmployeeNo(), savedUser.getName(), savedUser.getDepartment());
    }

    private void validateApprovedUser(User user, String employeeNo) {
        if (user.getStatus() == UserStatus.APPROVED) {
            return;
        }

        log.warn("Login blocked: User status is {} for employeeNo: {}", user.getStatus(), employeeNo);
        String message = switch (user.getStatus()) {
            case PENDING -> "User is pending approval.";
            case REJECTED -> "User signup was rejected.";
            case INACTIVE -> "User is inactive.";
            default -> "User cannot log in.";
        };
        throw new BaseException(ErrorCode.FORBIDDEN, message);
    }

    private TokenResponse buildTokenResponse(User user, String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .name(user.getName())
                .build();
    }
}
