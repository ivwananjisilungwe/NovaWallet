package com.novawallet.novawallet_api.audit.aspect;

import com.novawallet.novawallet_api.audit.annotation.Audited;
import com.novawallet.novawallet_api.audit.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * AOP aspect that intercepts {@link Audited @Audited} annotated methods
 * and automatically records audit logs via {@link AuditService}.
 * <p>
 * The aspect captures:
 * <ul>
 *   <li>Method arguments that represent an entity ID (any {@link UUID} parameter)</li>
 *   <li>A {@code performedBy} parameter if present as a {@link UUID} method argument
 *       (typically the authenticated user's ID)</li>
 *   <li>Optional "old" / "new" value snapshots from dedicated parameters named
 *       with {@code oldValue} / {@code newValue} in the annotation</li>
 * </ul>
 * <p>
 * Usage example on a service method:
 * <pre>{@code
 * @Audited(action = "ACTIVATE_WALLET", entityType = "Wallet")
 * public void activateWallet(UUID walletId, UUID performedBy) { ... }
 * }</pre>
 */
@Aspect
@Component
public class AuditedAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditedAspect.class);

    private final AuditService auditService;

    public AuditedAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Around advice: intercept {@code @Audited} methods, execute them,
     * and record an audit log entry with before/after snapshots.
     */
    @Around("@annotation(com.novawallet.novawallet_api.audit.annotation.Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        String action = audited.action();
        String entityType = audited.entityType();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        // Extract metadata from method parameters
        UUID entityId = extractEntityId(args, paramNames);
        UUID performedBy = extractPerformedBy(args, paramNames);
        String oldValue = extractOldValue(args, paramNames);
        String newValue = null;

        // Execute the target method
        try {
            Object result = joinPoint.proceed();

            // Try to extract newValue from result if it's a string-bearing object
            newValue = extractNewValue(result, args, paramNames);

            return result;
        } finally {
            // Record audit log (async) regardless of success or failure
            recordAudit(action, entityType, entityId, oldValue, newValue, performedBy);
        }
    }

    // ==================== Metadata Extraction ====================

    /**
     * Extract the entity ID from method arguments.
     * Uses the first UUID argument found that isn't {@code performedBy}.
     */
    private UUID extractEntityId(Object[] args, String[] paramNames) {
        if (args == null || paramNames == null) return null;

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if (args[i] instanceof UUID uuid
                    && !"performedBy".equals(paramNames[i])
                    && !"userId".equals(paramNames[i])) {
                return uuid;
            }
        }
        return null;
    }

    /**
     * Extract the {@code performedBy} user ID from method arguments.
     * Matches parameter named {@code performedBy} or {@code userId}.
     */
    private UUID extractPerformedBy(Object[] args, String[] paramNames) {
        if (args == null || paramNames == null) return null;

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if (args[i] instanceof UUID uuid
                    && ("performedBy".equals(paramNames[i])
                    || "userId".equals(paramNames[i]))) {
                return uuid;
            }
        }
        return null;
    }

    /**
     * Extract {@code oldValue} from a dedicated string parameter named {@code oldValue}.
     */
    private String extractOldValue(Object[] args, String[] paramNames) {
        if (args == null || paramNames == null) return null;

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if ("oldValue".equals(paramNames[i]) && args[i] instanceof String s) {
                return s;
            }
        }
        return null;
    }

    /**
     * Extract {@code newValue} from the method result or a dedicated parameter.
     */
    private String extractNewValue(Object result, Object[] args, String[] paramNames) {
        // First priority: explicit newValue parameter
        if (args != null && paramNames != null) {
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                if ("newValue".equals(paramNames[i]) && args[i] instanceof String s) {
                    return s;
                }
            }
        }

        // Fallback: if the result has a meaningful toString, use it
        if (result != null) {
            return Optional.of(result)
                    .map(Object::toString)
                    .filter(s -> !s.isEmpty())
                    .orElse(null);
        }

        return null;
    }

    /**
     * Record the audit log entry (delegates to the async AuditService).
     */
    private void recordAudit(
            String action, String entityType, UUID entityId,
            String oldValue, String newValue, UUID performedBy
    ) {
        try {
            auditService.recordAction(
                    entityType != null && !entityType.isEmpty()
                            ? entityType
                            : "Unknown",
                    entityId,
                    action,
                    oldValue,
                    newValue,
                    performedBy
            );
            log.debug("Audited action: {} on {} (id={})", action, entityType, entityId);
        } catch (Exception e) {
            log.warn("Failed to record audit for action={} entity={}: {}",
                    action, entityType, e.getMessage());
        }
    }
}
