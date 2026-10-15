package org.example.giic_asset.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.ClientIpResolver;
import org.example.giic_asset.service.UserAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private final UserAccessService userAccessService;
    private final ClientIpResolver clientIpResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute("requestId", requestId);

        log.info("[{}] Request: {} {} from {}", requestId, request.getMethod(), request.getRequestURI(),
                clientIpResolver.resolve(request));

        // Additional logging details
        if (request.getQueryString() != null) {
            log.info("[{}] Query String: {}", requestId, request.getQueryString());
        }
        log.info("[{}] Authorization: {}", requestId,
                request.getHeader("Authorization") != null ? "[PRESENT]" : "[ABSENT]");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            try {
                userAccessService.recordActivity(user.getUsername(), clientIpResolver.resolve(request));
            } catch (RuntimeException exception) {
                log.warn("Unable to record user activity for {}", user.getUsername(), exception);
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        // Can log post-processing here if needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        String requestId = (String) request.getAttribute("requestId");
        if (ex != null) {
            log.error("[{}] Response: {} {} - Exception: {}", requestId, response.getStatus(), request.getRequestURI(),
                    ex.getMessage());
        } else {
            log.info("[{}] Response: {} {}", requestId, response.getStatus(), request.getRequestURI());
        }
    }
}
