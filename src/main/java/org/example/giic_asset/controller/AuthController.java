package org.example.giic_asset.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.common.dto.ApiResponse;
import org.example.giic_asset.dto.auth.LoginRequest;
import org.example.giic_asset.dto.auth.RefreshTokenRequest;
import org.example.giic_asset.dto.auth.TokenResponse;
import org.example.giic_asset.dto.user.SignupRequest;
import org.example.giic_asset.service.AuthService;
import org.example.giic_asset.service.ClientIpResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request,
                                            HttpServletRequest servletRequest) {
        log.info("========== POST /api/auth/login ==========");
        log.info("Login request for employeeNo: {}", request.getEmployeeNo());
        TokenResponse response = authService.login(request, clientIpResolver.resolve(servletRequest));
        log.info("Login successful for user: {}, role: {}", response.getName(), response.getRole());
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        log.info("========== POST /api/auth/refresh ==========");
        TokenResponse response = authService.refreshToken(request.getRefreshToken());
        log.info("Token refresh successful for user: {}", response.getName());
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        log.info("========== POST /api/auth/logout ==========");
        authService.logout(extractBearerToken(authorizationHeader));
        log.info("Logout successful");
        return ApiResponse.success("Logged out successfully.");
    }

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody @Valid SignupRequest request) {
        log.info("========== POST /api/auth/signup ==========");
        log.info("Signup request for employeeNo: {}, name: {}, email: {}",
                request.getEmployeeNo(), request.getName(), request.getEmail());
        authService.signup(request);
        log.info("Signup successful");
        return ApiResponse.success("Signup completed.");
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization Bearer token is required.");
        }
        return authorizationHeader.substring(7);
    }
}
