package com.novawallet.novawallet_api.kyc.enums;

/**
 * KYC verification lifecycle for a user.
 *
 * <p>{@code NOT_SUBMITTED} — user has not submitted any KYC documents.<br>
 * {@code PENDING} — KYC documents submitted, awaiting admin review.<br>
 * {@code APPROVED} — KYC verification approved; tier limits apply.<br>
 * {@code REJECTED} — KYC verification rejected; user can re-submit.</p>
 */
public enum KycStatus {
    /** User has not submitted any KYC documents. */
        NOT_SUBMITTED,
    /** KYC documents submitted, awaiting admin review. */
        PENDING,
    /** KYC verification approved; tier limits apply. */
        APPROVED,
    REJECTED
}
