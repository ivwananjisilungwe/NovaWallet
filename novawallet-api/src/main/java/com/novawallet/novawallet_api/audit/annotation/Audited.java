package com.novawallet.novawallet_api.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit logging via AOP.
 * <p>
 * Usage: annotate any service method with {@code @Audited} to have its
 * invocations recorded in the audit_logs table automatically.
 * <p>
 * Example:
 * <pre>{@code
 * @Audited(action = "WALLET_ACTIVATE", entityType = "Wallet")
 * public void activateWallet(UUID walletId) { ... }
 * }</pre>
 * <p>
 * The {@link com.novawallet.novawallet_api.audit.aspect.AuditedAspect} intercepts
 * all {@code @Audited} methods, captures the old/new values by reading the
 * entity before and after the method executes, and records the audit log
 * via {@link com.novawallet.novawallet_api.audit.service.AuditService}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The business action being performed (e.g., "WALLET_ACTIVATE", "USER_ROLE_CHANGE").
     * This is stored in the audit log's {@code action} column.
     */
    String action();

    /**
     * The entity type being audited (e.g., "Wallet", "User", "FeeConfiguration").
     * This is stored in the audit log's {@code entity_type} column.
     */
    String entityType() default "";
}
