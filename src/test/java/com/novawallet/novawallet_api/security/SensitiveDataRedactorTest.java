package com.novawallet.novawallet_api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * SEC-03 / NEW-AUTH-01: the redactor must be null-safe, never throw, and
 * must mask URL token params, labelled secrets, and unlabelled token runs.
 */
class SensitiveDataRedactorTest {

    @Test
    @DisplayName("null input returns null")
    void nullReturnsNull() {
        assertThat(SensitiveDataRedactor.redact(null)).isNull();
    }

    @Test
    @DisplayName("blank input is returned unchanged")
    void blankIsUnchanged() {
        assertThat(SensitiveDataRedactor.redact("   ")).isEqualTo("   ");
        assertThat(SensitiveDataRedactor.redact("")).isEqualTo("");
    }

    @Test
    @DisplayName("URL token query parameter is masked")
    void masksUrlTokenParam() {
        String input = "https://example.com/verify?token=abc123DEF456ghi789jkl012mno345pqr";
        String result = SensitiveDataRedactor.redact(input);

        assertThat(result).contains("token=[REDACTED]");
        assertThat(result).doesNotContain("abc123DEF456ghi789jkl012mno345pqr");
    }

    @Test
    @DisplayName("resetToken and access_token query params are masked")
    void masksOtherQueryParams() {
        String input = "https://x/reset?resetToken=verylongtokenthatshouldberedacted123&access_token=anotherlongtoken456789abcdef";
        String result = SensitiveDataRedactor.redact(input);

        assertThat(result).contains("resetToken=[REDACTED]");
        assertThat(result).contains("access_token=[REDACTED]");
    }

    @Test
    @DisplayName("Labelled secret in free text is masked")
    void masksLabelledSecret() {
        String input = "Token: super-secret-value-that-is-long-enough-to-be-redacted";
        String result = SensitiveDataRedactor.redact(input);

        assertThat(result).contains("Token:");
        assertThat(result).doesNotContain("super-secret-value-that-is-long-enough-to-be-redacted");
    }

    @Test
    @DisplayName("A long token-like run is masked")
    void masksUnlabelledTokenRun() {
        String token = "AbCdEfGh1jKlMnOpQrStUvWxYz1234567890"; // mixed case + digits
        String result = SensitiveDataRedactor.redact("prefix " + token + " suffix");

        assertThat(result).contains("[REDACTED]");
        assertThat(result).doesNotContain(token);
    }

    @Test
    @DisplayName("Ordinary prose is returned unchanged")
    void ordinaryTextIsUnchanged() {
        String input = "Hello world. This is a normal sentence.";
        assertThat(SensitiveDataRedactor.redact(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("The redactor never throws, even on malformed input")
    void neverThrows() {
        assertThatCode(() -> SensitiveDataRedactor.redact("token=@@@###$$$%%%^^^&&&***"))
                .doesNotThrowAnyException();
        assertThatCode(() -> SensitiveDataRedactor.redact("a".repeat(10000)))
                .doesNotThrowAnyException();
    }
}
