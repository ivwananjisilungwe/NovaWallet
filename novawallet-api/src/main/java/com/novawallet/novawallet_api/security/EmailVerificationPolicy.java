package com.novawallet.novawallet_api.security;

import com.novawallet.novawallet_api.exception.ForbiddenException;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Single enforcement point for the "verified email required for money
 * movement" policy (Phase 1C, SEC-01/DEC-01).
 *
 * <p>DEC-01 deliberately scopes email-verification enforcement to financial
 * operations only. The policy is <strong>not</strong> applied globally in the
 * JWT filter: an unverified user must still be able to complete onboarding,
 * read their profile and balance, submit KYC documents, and receive
 * notifications. Every money-out entry point calls
 * {@link #requireVerifiedForMoneyMovement(java.util.UUID)} before any balance
 * mutation, so the rule cannot be bypassed by adding a new endpoint that
 * forgets the check — new paths must call this method explicitly.</p>
 */
@Component
public class EmailVerificationPolicy {

    static final String UNVERIFIED_EMAIL_MESSAGE =
            "Email not verified — please verify your email before moving funds.";

    private final UserRepository userRepository;

    public EmailVerificationPolicy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Rejects the operation with HTTP 403 when the user has not verified
     * their email address.
     *
     * @param userId the authenticated user
     * @throws ForbiddenException      when the email is not verified
     * @throws ResourceNotFoundException when the user no longer exists
     */
    public void requireVerifiedForMoneyMovement(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new ForbiddenException(UNVERIFIED_EMAIL_MESSAGE);
        }
    }
}
