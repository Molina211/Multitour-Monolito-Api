package com.corhuila.errorcapa8.travesia_natural.common.security;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Without this bean, Spring Security 6 falls back to {@code Http403ForbiddenEntryPoint}
 * for any route protected by {@code .authenticated()} with no token at all — spec 007's
 * acceptance criteria require {@code 401} for "missing/invalid token" and reserve
 * {@code 403} for "valid token, wrong tenant" (handled separately, in the controller).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErrorResponse("unauthorized", "missing or invalid token")));
    }
}
