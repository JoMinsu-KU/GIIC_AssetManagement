package org.example.giic_asset.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute("requestId", requestId);

        log.info("[{}] Request: {} {} from {}", requestId, request.getMethod(), request.getRequestURI(),
                request.getRemoteAddr());

        // Additional logging details
        if (request.getQueryString() != null) {
            log.info("[{}] Query String: {}", requestId, request.getQueryString());
        }
        log.info("[{}] Authorization: {}", requestId,
                request.getHeader("Authorization") != null ? "[PRESENT]" : "[ABSENT]");

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
