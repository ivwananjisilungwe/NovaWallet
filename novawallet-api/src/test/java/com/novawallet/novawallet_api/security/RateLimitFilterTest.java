package com.novawallet.novawallet_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        // Use a very tight limit (2 requests per 60s) so tests are fast
        filter = new RateLimitFilter(2, 2, 60);
    }

    @Test
    @DisplayName("Requests under the limit should pass through")
    void underLimit_shouldProceed() throws Exception {
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/v1/wallets/balance");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    @DisplayName("Exceeding the limit should return 429 with correct headers")
    void overLimit_shouldReturn429() throws Exception {
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getRequestURI()).thenReturn("/v1/transactions");
        when(response.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

        // First request — OK
        filter.doFilterInternal(request, response, chain);

        // Reset the chain mock so we can verify second call
        reset(chain);

        // Second request — OK (at limit)
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);

        // Third request — exceeds limit
        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(429);
        verify(response).setHeader(eq("Retry-After"), eq("60"));
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("X-RateLimit headers should reflect remaining capacity")
    void rateLimitHeaders_shouldBeSet() throws Exception {
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(request.getRequestURI()).thenReturn("/v1/wallets/balance");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-RateLimit-Limit", "2");
        verify(response).setHeader("X-RateLimit-Remaining", "1");
        verify(response).setHeader("X-RateLimit-Reset", "60");
    }

    @Test
    @DisplayName("Auth endpoints should use a stricter limit")
    void authEndpoint_shouldUseStricterLimit() throws Exception {
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(request.getRequestURI()).thenReturn("/v1/auth/login");
        when(response.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

        // First request
        filter.doFilterInternal(request, response, chain);
        reset(chain);

        // Second request (at auth limit of 2)
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);

        // Third request — exceeds auth limit
        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(429);
    }

    @Test
    @DisplayName("Different IPs should have independent counters")
    void differentIps_shouldHaveIndependentCounters() throws Exception {
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/v1/wallets/balance");

        // IP A uses its 2 requests
        when(request.getRemoteAddr()).thenReturn("10.0.0.10");
        filter.doFilterInternal(request, response, chain);
        filter.doFilterInternal(request, response, chain);
        reset(chain);

        // IP B's first request — should still be OK (independent counter)
        when(request.getRemoteAddr()).thenReturn("10.0.0.20");
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Response should include error JSON body when rate limited")
    void rateLimitedResponse_shouldContainJsonBody() throws Exception {
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.99");
        when(request.getRequestURI()).thenReturn("/v1/limits/test");
        var writer = mock(java.io.PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        // Send 3 requests to exceed limit of 2
        filter.doFilterInternal(request, response, chain);
        filter.doFilterInternal(request, response, chain);
        filter.doFilterInternal(request, response, chain);

        verify(writer).write(contains("\"status\":429"));
        verify(writer).write(contains("\"code\":\"RATE_LIMITED\""));
        verify(writer).write(contains("\"path\":\"/v1/limits/test\""));
    }
}
