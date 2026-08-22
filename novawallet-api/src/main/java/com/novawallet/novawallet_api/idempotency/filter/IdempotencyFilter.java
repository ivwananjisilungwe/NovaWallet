package com.novawallet.novawallet_api.idempotency.filter;

import com.novawallet.novawallet_api.idempotency.entity.IdempotencyKey;
import com.novawallet.novawallet_api.idempotency.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servlet filter that enforces idempotency for mutating HTTP methods (POST, PUT,
 * PATCH, DELETE) by inspecting the {@code Idempotency-Key} request header.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li><b>Skip</b> – Non-mutating methods (GET, HEAD, OPTIONS, TRACE) and
 *       requests without the {@code Idempotency-Key} header pass through
 *       unmodified.</li>
 *   <li><b>Scope</b> – The authenticated user is resolved from the security
 *       context (this filter runs after {@code JwtAuthFilter}, see
 *       {@code SecurityConfig}) and the raw key is scoped to
 *       {@code "<userId>:<key>"} via {@link IdempotencyService#scopedKey}.
 *       Unauthenticated requests keep the raw key so public endpoints retain
 *       global dedup.</li>
 *   <li><b>Acquire</b> – The filter atomically inserts a PROCESSING row via
 *       {@link IdempotencyService#tryAcquire}. If the insert succeeds, this
 *       request "owns" the key.</li>
 *   <li><b>Process</b> – The owning request passes through
 *       {@link jakarta.servlet.FilterChain#doFilter} and the response body is
 *       captured with a {@link ContentCachingResponseWrapper}.</li>
 *   <li><b>Complete</b> – After a successful (2xx) response the filter persists
 *       the status code and body via {@link IdempotencyService#complete}.</li>
 *   <li><b>Replay</b> – If acquire fails (duplicate key), the filter polls the
 *       DB briefly for the completed record and returns the cached response
 *       as-is, guaranteeing the caller sees exactly the same result.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * The database unique constraint on the stored (scoped) idempotency key value
 * combined with {@code REQUIRES_NEW} propagation provides safe concurrency
 * across any number of application instances.
 *
 * <h3>Error handling</h3>
 * If the owning request fails (non-2xx), the PROCESSING record is deleted so
 * the caller can retry with the same key. If the replay poll times out, a
 * 409 Conflict is returned. Keys longer than {@value #MAX_KEY_LENGTH} chars are
 * rejected with 400 so the scoped value always fits the VARCHAR(255) column.
 *
 * <h3>Endpoint scope</h3>
 * All mutating endpoints implicitly support idempotency. Clients opt in by
 * sending the header. Public endpoints (auth/register, etc.) are also covered
 * with global (unscoped) keys – useful for future webhook deduplication (I5).
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final Set<String> NON_MUTATING = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.TRACE.name()
    );
    /**
     * Upper bound for the raw client-supplied key. The stored value is
     * {@code <36-char uuid>:<key>} and must fit VARCHAR(255); 200 leaves ample
     * headroom while keeping oversized keys from failing as confusing 409s.
     */
    private static final int MAX_KEY_LENGTH = 200;
    private static final int MAX_POLL_ATTEMPTS = 10;
    private static final long POLL_INTERVAL_MS = 100;

    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // ---- Step 1: Skip non-mutating methods ----
        if (NON_MUTATING.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // ---- Step 2: Extract header ----
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // Oversized keys would overflow the VARCHAR(255) column once scoped
        // with the user id prefix – reject them explicitly instead of failing
        // later as a misleading 409.
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            log.warn("Rejecting oversized Idempotency-Key header ({} chars)", idempotencyKey.length());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"success":false,"message":"Idempotency-Key must be at most %d characters.","code":"IDEMPOTENCY_KEY_TOO_LONG"}
                    """.formatted(MAX_KEY_LENGTH));
            return;
        }

        // ---- Step 3: Resolve the authenticated user and scope the key ----
        UUID userId = resolveAuthenticatedUserId();
        String scopedKey = IdempotencyService.scopedKey(userId, idempotencyKey);

        // ---- Step 4: Try to acquire the key (atomic insert) ----
        boolean acquired = idempotencyService.tryAcquire(
                scopedKey,
                request.getMethod(),
                request.getRequestURI(),
                userId
        );

        if (acquired) {
            // ---- Step 5a: OWNING REQUEST – process normally, capture response ----
            ContentCachingResponseWrapper wrappedResponse =
                    new ContentCachingResponseWrapper(response);
            try {
                chain.doFilter(request, wrappedResponse);

                int status = wrappedResponse.getStatus();
                byte[] bodyBytes = wrappedResponse.getContentAsByteArray();
                String body = new String(bodyBytes, wrappedResponse.getCharacterEncoding() != null
                        ? wrappedResponse.getCharacterEncoding() : StandardCharsets.UTF_8.name());

                // Copy cached content to the real output stream
                wrappedResponse.copyBodyToResponse();

                // Save response only for successful (2xx) outcomes
                if (status >= 200 && status < 300) {
                    idempotencyService.complete(scopedKey, status, body);
                } else {
                    // Non-2xx → delete the PROCESSING record so the client retries
                    deleteProcessingRecord(scopedKey);
                }
            } catch (Exception e) {
                deleteProcessingRecord(scopedKey);
                throw e;
            }
        } else {
            // ---- Step 5b: DUPLICATE – poll for the completed record ----
            serveCachedResponse(idempotencyKey, scopedKey, request, response);
        }
    }

    /**
     * Resolves the authenticated user's id from the security context. This
     * filter is registered after {@code JwtAuthFilter} in {@code SecurityConfig},
     * so the context is already populated for authenticated requests. The
     * principal's username carries the user id (see
     * {@code CustomUserDetailsService#loadUserById}). Returns null for
     * anonymous requests, which keeps their keys globally scoped.
     */
    private UUID resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }
        try {
            return UUID.fromString(userDetails.getUsername());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Polls the DB for a completed idempotency record and replays the cached
     * response to the caller. Lookups use the scoped storage key; the raw key
     * is kept for log messages.
     */
    private void serveCachedResponse(String rawKey,
                                     String scopedKey,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            Optional<IdempotencyKey> cached = idempotencyService.getCachedResponse(scopedKey);
            if (cached.isPresent()) {
                replay(cached.get(), response);
                return;
            }

            // Still PROCESSING – wait and retry
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // ---- Timeout – return 409 Conflict ----
        log.warn("Idempotency key '{}' still PROCESSING after {} polls – returning 409", rawKey, MAX_POLL_ATTEMPTS);
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"success":false,"message":"Request is already being processed. Try again shortly.","code":"IDEMPOTENCY_IN_FLIGHT"}
                """);
    }

    /**
     * Writes the cached response body and status to {@code response}.
     */
    private void replay(IdempotencyKey record, HttpServletResponse response) throws IOException {
        log.debug("Replaying cached response for idempotency key: {}", record.getIdempotencyKey());
        response.setStatus(record.getResponseStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    /**
     * Removes the PROCESSING record (looked up by scoped storage key) so the
     * client can retry with the same key.
     */
    private void deleteProcessingRecord(String scopedKey) {
        try {
            idempotencyService.deleteProcessing(scopedKey);
            log.debug("Deleted PROCESSING record for key '{}' after non-2xx response", scopedKey);
        } catch (Exception e) {
            log.warn("Failed to clean up PROCESSING record for key '{}'", scopedKey, e);
        }
    }
}
