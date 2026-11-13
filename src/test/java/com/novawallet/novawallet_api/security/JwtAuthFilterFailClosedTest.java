package com.novawallet.novawallet_api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novawallet.novawallet_api.token.service.AccessTokenRevocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DEC-02: when the revocation store cannot be read, authentication must fail
 * closed with 503. A revocable token must never be accepted because the
 * deny-list is unavailable.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterFailClosedTest {

    private static final String TOKEN = "a.b.c";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AccessTokenRevocationService revocationService;

    private JwtAuthFilter filter;
    private final ObjectMapper objectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .build();

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil, userDetailsService, revocationService, objectMapper);
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private JwtClaims claims() {
        return new JwtClaims(
                UUID.randomUUID(),
                "user@example.com",
                "USER",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(900)
        );
    }

    @Test
    @DisplayName("Revocation-store failure produces 503 and does not continue the chain")
    void storeFailureFailsClosed() throws Exception {
        when(jwtUtil.parseToken(TOKEN)).thenReturn(claims());
        when(revocationService.isRevoked(any()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("db down"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(requestWithBearerToken(), response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).contains("application/json");

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/users/me");

        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserById(any());
    }

    @Test
    @DisplayName("A revoked token is unauthenticated but the chain still runs")
    void revokedTokenLeavesRequestUnauthenticated() throws Exception {
        when(jwtUtil.parseToken(TOKEN)).thenReturn(claims());
        when(revocationService.isRevoked(any())).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(requestWithBearerToken(), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserById(any());
    }

    @Test
    @DisplayName("An invalid token is unauthenticated and never consults the store")
    void invalidTokenSkipsRevocationCheck() throws Exception {
        when(jwtUtil.parseToken(TOKEN)).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(requestWithBearerToken(), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(revocationService, never()).isRevoked(any());
    }

    @Test
    @DisplayName("A live token for a deleted user is unauthenticated, not a 503")
    void deletedUserLeavesRequestUnauthenticated() throws Exception {
        when(jwtUtil.parseToken(TOKEN)).thenReturn(claims());
        when(revocationService.isRevoked(any())).thenReturn(false);
        when(userDetailsService.loadUserById(any()))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("gone"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(requestWithBearerToken(), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("No bearer header means no parsing and no store lookup")
    void missingTokenPassesThrough() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/users/me"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(jwtUtil, never()).parseToken(any());
        verify(revocationService, never()).isRevoked(any());
    }
}
