package com.novawallet.novawallet_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novawallet.novawallet_api.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Returns a JSON 401 response for unauthenticated requests.
 *
 * <p>Spring Security's default entry point answers 403, which conflicts with
 * the API contract used by clients for refresh-on-401 handling. Authentication
 * failures (missing, expired, invalid or revoked tokens) are therefore
 * reported as 401; authorization failures remain 403.</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication required",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}