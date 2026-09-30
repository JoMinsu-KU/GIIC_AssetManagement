package org.example.giic_asset.config;

import org.example.giic_asset.security.JwtAuthenticationEntryPoint;
import org.example.giic_asset.security.JwtTokenProvider;
import org.example.giic_asset.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    void allowsConfiguredExternalFrontendOriginOnly() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtTokenProvider.class),
                mock(JwtAuthenticationEntryPoint.class),
                mock(RateLimitFilter.class));
        ReflectionTestUtils.setField(
                securityConfig,
                "allowedOrigins",
                "http://localhost:3000,http://203.253.164.48:3000");

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        CorsConfiguration cors = securityConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("http://203.253.164.48:3000"))
                .isEqualTo("http://203.253.164.48:3000");
        assertThat(cors.checkOrigin("http://malicious.example:3000")).isNull();
        assertThat(cors.getAllowedMethods()).contains("POST", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
