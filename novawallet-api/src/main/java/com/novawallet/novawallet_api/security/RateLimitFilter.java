package com.novawallet.novawallet_api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("!test")
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int defaultMaxRequests;
    private final int authMaxRequests;
    private final Duration window;

    private final Cache<String, AtomicInteger> counters;

    public RateLimitFilter(
            @Value("${app.rate-limit.global.max-requests:100}") int defaultMaxRequests,
            @Value("${app.rate-limit.global.auth-max-requests:10}") int authMaxRequests,
            @Value("${app.rate-limit.global.window-seconds:60}") int windowSeconds
    ) {
        this.defaultMaxRequests = defaultMaxRequests;
        this.authMaxRequests = authMaxRequests;
        this.window = Duration.ofSeconds(windowSeconds);

        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(50_000)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String clientKey = resolveClientKey(request);
        int limit = isAuthEndpoint(request) ? authMaxRequests : defaultMaxRequests;

        AtomicInteger counter = counters.get(clientKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - current)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(window.toSeconds()));

        if (current > limit) {
            log.warn("Rate limit exceeded for key='{}' ({}/{} {}s window)", clientKey, current, limit, window.toSeconds());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again later.\",\"code\":\"RATE_LIMITED\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String userId = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : null;
        String ip = request.getRemoteAddr();
        return userId != null ? "user:" + userId : "ip:" + ip;
    }

    private boolean isAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/v1/auth/") || path.contains("/v1/password/")
                || path.contains("/v1/pin/") || path.contains("/v1/email/");
    }
}
