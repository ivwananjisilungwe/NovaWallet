package com.novawallet.novawallet_api.idempotency.filter;

import com.novawallet.novawallet_api.idempotency.entity.IdempotencyKey;
import com.novawallet.novawallet_api.idempotency.entity.IdempotencyStatus;
import com.novawallet.novawallet_api.idempotency.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
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
 * The database unique constraint on {@code idempotency_key} combined with
 * {@code REQUIRES_NEW} propagation provides safe concurrency across any number
 * of application instances.
 *
 * <h3>Error handling</h3>
 * If the owning request fails (non-2xx), the PROCESSING record is deleted so
 * the caller can retry with the same key. If the replay poll times out, a
 * 409 Conflict is returned.
 *
 * <h3>Endpoint scope</h3>
 * All mutating endpoints implicitly support idempotency. Clients opt in by
 * sending the header. Public endpoints (auth/register, etc.) are also covered
 * – useful for future webhook deduplication (I5).
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

        // ---- Step 3: Extract user if available ----
        UUID userId = null;
        if (request.getUserPrincipal() != null) {
            try {
                // The user principal name is the email (set by JwtAuthFilter).
                // We store userId only if it can be parsed from a custom attribute.
                // For simplicity, userId is optional.
                Object principal = request.getUserPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                    // Could extract from username if it's a UUID, but skip for now
                }
            } catch (Exception e) {
                // Ignore – userId is not critical
            }
        }

        // ---- Step 4: Try to acquire the key (atomic insert) ----
        boolean acquired = idempotencyService.tryAcquire(
                idempotencyKey,
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
                    idempotencyService.complete(idempotencyKey, status, body);
                } else {
                    // Non-2xx → delete the PROCESSING record so the client retries
                    deleteProcessingRecord(idempotencyKey);
                }
            } catch (Exception e) {
                deleteProcessingRecord(idempotencyKey);
                throw e;
            }
        } else {
            // ---- Step 5b: DUPLICATE – poll for the completed record ----
            serveCachedResponse(idempotencyKey, request, response);
        }
    }

    /**
     * Polls the DB for a completed idempotency record and replays the cached
     * response to the caller.
     */
    private void serveCachedResponse(String key,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            Optional<IdempotencyKey> cached = idempotencyService.getCachedResponse(key);
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
        log.warn("Idempotency key '{}' still PROCESSING after {} polls – returning 409", key, MAX_POLL_ATTEMPTS);
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
     * Removes the PROCESSING record so the client can retry with the same key.
     */
    private void deleteProcessingRecord(String key) {
        try {
            idempotencyService.deleteProcessing(key);
            log.debug("Deleted PROCESSING record for key '{}' after non-2xx response", key);
        } catch (Exception e) {
            log.warn("Failed to clean up PROCESSING record for key '{}'", key, e);
        }
    }
}
