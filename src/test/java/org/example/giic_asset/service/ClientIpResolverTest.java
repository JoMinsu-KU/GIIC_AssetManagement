package org.example.giic_asset.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {
    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void usesNginxClientIpOnlyWhenRequestArrivesFromDockerProxy() {
        HttpServletRequest fromProxy = mock(HttpServletRequest.class);
        when(fromProxy.getRemoteAddr()).thenReturn("172.18.0.4");
        when(fromProxy.getHeader("X-Real-IP")).thenReturn("192.168.1.25");
        assertThat(resolver.resolve(fromProxy)).isEqualTo("192.168.1.25");

        HttpServletRequest direct = mock(HttpServletRequest.class);
        when(direct.getRemoteAddr()).thenReturn("192.168.1.40");
        when(direct.getHeader("X-Real-IP")).thenReturn("1.2.3.4");
        assertThat(resolver.resolve(direct)).isEqualTo("192.168.1.40");

        HttpServletRequest dockerGateway = mock(HttpServletRequest.class);
        when(dockerGateway.getRemoteAddr()).thenReturn("172.18.0.4");
        when(dockerGateway.getHeader("X-Real-IP")).thenReturn("172.18.0.1");
        assertThat(resolver.resolve(dockerGateway)).isNull();
    }
}
