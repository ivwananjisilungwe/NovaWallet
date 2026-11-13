package com.novawallet.novawallet_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novawallet.novawallet_api.exception.ErrorResponse;
import com.novawallet.novawallet_api.token.service.AccessTokenRevocationService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Extracts the bearer token, validates it, checks the durable revocation
 * store, and populates the security context.
 *
 * <p>Behaviour introduced in Phase 1C Batch 1:</p>
 * <ul>
 *   <li>The token is parsed once; the resulting {@link JwtClaims} are exposed
 *       as a request attribute so controllers (for example logout) can read
 *       the {@code jti} and expiry without parsing the credential again.</li>
 *   <li>Revoked tokens are treated as unauthenticated. The request continues
 *       down the chain and Spring Security answers 401 for protected
 *       endpoints, exactly as it does for an invalid signature.</li>
 *   <li>If the revocation store cannot be read, the filter <strong>fails
 *       closed</strong> with HTTP 503 instead of allowing a possibly-revoked
 *       token through (DEC-02). This response is written here because
 *       exceptions raised inside a servlet filter never reach
 *       {@code @RestControllerAdvice}.</li>
 * </ul>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Request attribute holding the validated {@link JwtClaims}. */
    public static final String JWT_CLAIMS_ATTRIBUTE = JwtClaims.class.getName();

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final AccessTokenRevocationService revocationService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            AccessTokenRevocationService revocationService,
            ObjectMapper objectMapper
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.revocationService = revocationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtClaims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid, expired or non-conforming token: leave the request
            // unauthenticated. Protected endpoints will answer 401.
            log.debug("Rejected access token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        boolean revoked;
        try {
            revoked = revocationService.isRevoked(claims.tokenId());
        } catch (RuntimeException e) {
            // Fail closed: an unavailable revocation store must never cause a
            // revoked token to be accepted.
            log.error("Access-token revocation store unavailable — rejecting request with 503", e);
            writeServiceUnavailable(request, response);
            return;
        }

        if (revoked) {
            log.debug("Rejected revoked access token: jti={}", claims.tokenId());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserById(claims.userId());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(JWT_CLAIMS_ATTRIBUTE, claims);
        } catch (Exception e) {
            log.warn("Failed to authenticate JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void writeServiceUnavailable(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "SERVICE_UNAVAILABLE",
                "Authentication is temporarily unavailable. Please retry shortly.",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
