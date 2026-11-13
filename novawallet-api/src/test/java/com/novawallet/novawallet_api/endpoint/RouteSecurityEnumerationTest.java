package com.novawallet.novawallet_api.endpoint;

import com.novawallet.novawallet_api.auth.controller.BaseAuthIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Phase 1C Batch 2 — route-level default-deny check.
 *
 * <p>Enumerates every request mapping the application registers, sends each one
 * anonymously, and requires 401 unless the route is in the explicit public
 * allow-list below. The allow-list mirrors the {@code permitAll()} rules in
 * {@code SecurityConfig}; if one side changes, this test fails until the other
 * side is updated too.</p>
 *
 * <p>Skipped mappings (documented, not app controllers):</p>
 * <ul>
 *   <li>{@code org.springdoc.*} — springdoc-openapi endpoints for {@code /api/api-docs}
 *       and the Swagger UI. {@code SecurityConfig} permits them explicitly; they are
 *       not registered under the app's controller package.</li>
 *   <li>{@code org.springframework.boot.autoconfigure.web.servlet.error.*} —
 *       {@code BasicErrorController} for {@code /error}.</li>
 * </ul>
 *
 * <p>Not enumerated because they are not {@code RequestMappingHandlerMapping}
 * routes, but still permitted in {@code SecurityConfig}: {@code /actuator/health},
 * {@code /actuator/info}, {@code /api/swagger-ui/**}, {@code /api/swagger-ui.html},
 * {@code /api/api-docs/**}, {@code /webjars/**}.</p>
 */
class RouteSecurityEnumerationTest extends BaseAuthIntegrationTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private static final String APP_PACKAGE = "com.novawallet.novawallet_api.";

    /** Bean-class package prefixes that are intentionally not checked (see class Javadoc). */
    private static final List<String> SKIPPED_BEAN_PACKAGES = List.of(
            "org.springdoc.",
            "org.springframework.boot.autoconfigure.web.servlet.error."
    );

    /** Exact paths permitted anonymously in SecurityConfig. */
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/password/forgot",
            "/api/v1/password/reset",
            "/api/v1/email/verify"
    );

    /** SecurityConfig: {@code /api/v1/webhooks/lipila/**} is permitAll (shared-secret auth in the controller). */
    private static final String PUBLIC_PREFIX = "/api/v1/webhooks/lipila/";

    /** Every allow-listed app route must still be registered; otherwise the allow-list is stale. */
    private static final List<String> REQUIRED_PUBLIC_ROUTES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/password/forgot",
            "/api/v1/password/reset",
            "/api/v1/email/verify",
            "/api/v1/webhooks/lipila/collections",
            "/api/v1/webhooks/lipila/disbursements"
    );

    /** Sanity floor so the enumeration cannot pass vacuously. Current app registers about 48 checked routes. */
    private static final int MIN_CHECKED_ROUTES = 40;

    @Test
    @DisplayName("every app route answers anonymous callers with 401 unless it is explicitly public")
    void everyNonPublicRouteRequiresAuthentication() {
        List<String> failures = new ArrayList<>();
        Set<String> mappedPatterns = new TreeSet<>();
        int checked = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            Class<?> beanType = entry.getValue().getBeanType();
            String beanName = beanType.getName();

            if (SKIPPED_BEAN_PACKAGES.stream().anyMatch(beanName::startsWith)) {
                continue;
            }
            if (!beanName.startsWith(APP_PACKAGE)) {
                failures.add("Unexpected non-app mapping handled by " + beanName
                        + " (add to SKIPPED_BEAN_PACKAGES only if it is a known framework endpoint)");
                continue;
            }

            Set<String> methods = entry.getKey().getMethodsCondition().getMethods().stream()
                    .map(m -> m.name())
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            if (methods.isEmpty()) {
                failures.add("Mapping without an explicit HTTP method cannot be enumerated: " + entry.getValue());
                continue;
            }

            for (String pattern : patternsOf(entry.getKey())) {
                mappedPatterns.add(pattern);
                if (isPublic(pattern)) {
                    continue;
                }
                String path = pattern.replaceAll("\\{[^/}]+}", UUID.randomUUID().toString());

                for (String method : methods) {
                    checked++;
                    try {
                        int status = anonymousStatus(HttpMethod.valueOf(method), path);
                        if (status != 401) {
                            failures.add(String.format("%s %s (handler %s) -> %d, expected 401",
                                    method, pattern, beanName, status));
                        }
                    } catch (Exception e) {
                        failures.add(String.format("%s %s (handler %s) -> threw %s",
                                method, pattern, beanName, e.getClass().getSimpleName()));
                    }
                }
            }
        }

        for (String route : REQUIRED_PUBLIC_ROUTES) {
            if (!mappedPatterns.contains(route)) {
                failures.add("Allow-listed public route is no longer mapped: " + route
                        + " (update the allow-list and SecurityConfig together)");
            }
        }

        if (checked < MIN_CHECKED_ROUTES) {
            failures.add("Only " + checked + " route/method pairs were checked; expected at least "
                    + MIN_CHECKED_ROUTES + ". The enumeration may be broken.");
        }

        assertThat(failures)
                .as("Route security enumeration found %d problem(s) across %d checked route/method pairs:%n%s",
                        failures.size(), checked, String.join(System.lineSeparator(), failures))
                .isEmpty();
    }

    private int anonymousStatus(HttpMethod method, String path) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, path);
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            builder = builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        return result.getResponse().getStatus();
    }

    private static boolean isPublic(String pattern) {
        return PUBLIC_EXACT_PATHS.contains(pattern) || pattern.startsWith(PUBLIC_PREFIX);
    }

    private static Set<String> patternsOf(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatternValues();
        }
        return info.getPatternsCondition().getPatterns();
    }
}
