package com.novawallet.novawallet_api.user.entity;

/**
 * Roles for role-based access control.
 *
 * <p>{@code USER} — standard platform user with access to own wallet and transactions.<br>
 * {@code ADMIN} — platform administrator with access to all admin endpoints.</p>
 */
public enum Role {
    /** Standard platform user with access to own wallet and transactions. */
        USER,
    ADMIN
}
