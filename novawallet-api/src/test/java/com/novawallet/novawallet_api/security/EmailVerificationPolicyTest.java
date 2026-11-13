package com.novawallet.novawallet_api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * SEC-01 / DEC-01: email verification is enforced at financial entry points
 * and nowhere else. The policy itself must be strict and must not silently
 * accept an unknown user.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationPolicyTest {

    private static final java.util.UUID USER_ID = java.util.UUID.randomUUID();

    @Mock
    private com.novawallet.novawallet_api.user.repository.UserRepository userRepository;

    @InjectMocks
    private EmailVerificationPolicy policy;

    private com.novawallet.novawallet_api.user.entity.User user(boolean verified) {
        return com.novawallet.novawallet_api.user.entity.User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .emailVerified(verified)
                .build();
    }

    @Test
    @DisplayName("Verified user passes the money-movement gate")
    void verifiedUserPasses() {
        when(userRepository.findById(USER_ID))
                .thenReturn(java.util.Optional.of(user(true)));

        assertThatCode(() -> policy.requireVerifiedForMoneyMovement(USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Unverified user is rejected with 403 (ForbiddenException)")
    void unverifiedUserIsForbidden() {
        when(userRepository.findById(USER_ID))
                .thenReturn(java.util.Optional.of(user(false)));

        assertThatThrownBy(() -> policy.requireVerifiedForMoneyMovement(USER_ID))
                .isInstanceOf(com.novawallet.novawallet_api.exception.ForbiddenException.class)
                .hasMessageContaining("Email not verified");
    }

    @Test
    @DisplayName("A user that no longer exists is a 404, never a silent pass")
    void missingUserIsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> policy.requireVerifiedForMoneyMovement(USER_ID))
                .isInstanceOf(com.novawallet.novawallet_api.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("The rejection message is stable so clients can detect it")
    void messageIsStable() {
        when(userRepository.findById(USER_ID))
                .thenReturn(java.util.Optional.of(user(false)));

        assertThatThrownBy(() -> policy.requireVerifiedForMoneyMovement(USER_ID))
                .hasMessage(EmailVerificationPolicy.UNVERIFIED_EMAIL_MESSAGE);
    }
}
