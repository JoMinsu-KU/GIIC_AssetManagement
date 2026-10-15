package org.example.giic_asset.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.giic_asset.service.ClientIpResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ClientIpResolver clientIpResolver;
    private final Map<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isRateLimitedPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientKey(request);
        RequestWindow window = requestWindows.compute(key, (ignored, current) -> nextWindow(current));

        if (window.count > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/auth/login".equals(path)
                || "/api/auth/signup".equals(path)
                || "/api/auth/refresh".equals(path));
    }

    private RequestWindow nextWindow(RequestWindow current) {
        long now = Instant.now().toEpochMilli();
        if (current == null || current.expiresAtMillis <= now) {
            return new RequestWindow(1, now + WINDOW_MILLIS);
        }
        current.count++;
        return current;
    }

    private String clientKey(HttpServletRequest request) {
        String resolved = clientIpResolver.resolve(request);
        return resolved == null ? request.getRemoteAddr() : resolved;
    }

    private static class RequestWindow {
        private int count;
        private final long expiresAtMillis;

        private RequestWindow(int count, long expiresAtMillis) {
            this.count = count;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
