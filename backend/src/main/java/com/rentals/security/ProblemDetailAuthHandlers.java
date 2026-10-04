package com.rentals.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns ProblemDetail (RFC 9457) responses for 401 and 403 errors.
 * ARCHITECTURE §4.2: ProblemDetailAuthHandlers.
 */
@Component
public class ProblemDetailAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        writeProblemDetail(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
            "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        writeProblemDetail(response, HttpStatus.FORBIDDEN, "Forbidden",
            "Insufficient permissions");
    }

    private void writeProblemDetail(HttpServletResponse response, HttpStatus status,
                                    String title, String detail) throws IOException {
        String json = String.format(
            "{\"type\":\"about:blank\",\"status\":%d,\"title\":\"%s\",\"detail\":\"%s\"}",
            status.value(), title, detail);
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write(json);
    }
}
