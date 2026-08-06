package com.novawallet.novawallet_api.kyc.enums;

/**
 * Verification status for an uploaded KYC document.</p>
 */
public enum KycDocumentStatus {
    /** Document uploaded, awaiting admin review. */
        PENDING,
    /** Document verified and approved. */
        APPROVED,
    REJECTED
}
