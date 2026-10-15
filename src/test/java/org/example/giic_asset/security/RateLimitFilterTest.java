package org.example.giic_asset.security;

import org.example.giic_asset.service.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {
    @Test
    void changingUntrustedForwardedHeadersDoesNotBypassTheLoginLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ClientIpResolver(true));

        for (int attempt = 1; attempt <= 21; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("192.168.1.40");
            request.addHeader("X-Forwarded-For", "203.0.113." + attempt);
            request.addHeader("X-Real-IP", "198.51.100." + attempt);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});
            assertThat(response.getStatus()).isEqualTo(attempt <= 20 ? 200 : 429);
        }
    }
}
