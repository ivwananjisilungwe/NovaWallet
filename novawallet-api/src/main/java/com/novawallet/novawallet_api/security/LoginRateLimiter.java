package com.novawallet.novawallet_api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private final int maxAttempts;
    private final Duration lockoutDuration;
    private final Cache<String, AtomicInteger> attemptCache;
    private final Cache<String, Long> lockoutCache;

    public LoginRateLimiter(
            @Value("${app.rate-limit.login.max-attempts:5}") int maxAttempts,
            @Value("${app.rate-limit.login.lockout-minutes:15}") int lockoutMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);

        this.attemptCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(lockoutMinutes))
                .maximumSize(10_000)
                .build();

        this.lockoutCache = Caffeine.newBuilder()
                .expireAfterWrite(lockoutDuration)
                .maximumSize(10_000)
                .build();
    }

    public void recordFailedAttempt(String email, HttpServletRequest request) {
        String key = buildKey(email, request);
        AtomicInteger attempts = attemptCache.get(key, k -> new AtomicInteger(0));
        int current = attempts.incrementAndGet();

        log.warn("Failed login attempt for key='{}' ({}/{})", key, current, maxAttempts);

        if (current >= maxAttempts) {
            lockoutCache.put(key, System.currentTimeMillis());
            log.warn("Login locked for key='{}' for {} minutes", key, lockoutDuration.toMinutes());
        }
    }

    public void recordSuccessfulAttempt(String email, HttpServletRequest request) {
        String key = buildKey(email, request);
        attemptCache.invalidate(key);
        lockoutCache.invalidate(key);
    }

    public boolean isLocked(String email, HttpServletRequest request) {
        String key = buildKey(email, request);
        Long lockTime = lockoutCache.getIfPresent(key);
        if (lockTime != null) {
            log.warn("Blocked login attempt for locked key='{}'", key);
            return true;
        }
        return false;
    }

    public int getRemainingAttempts(String email, HttpServletRequest request) {
        String key = buildKey(email, request);
        AtomicInteger attempts = attemptCache.getIfPresent(key);
        if (attempts == null) return maxAttempts;
        return Math.max(0, maxAttempts - attempts.get());
    }

    private String buildKey(String email, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return email.toLowerCase().trim() + "|" + ip;
    }
}
