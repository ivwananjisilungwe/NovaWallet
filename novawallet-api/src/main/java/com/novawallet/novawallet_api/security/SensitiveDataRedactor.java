package com.novawallet.novawallet_api.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defence-in-depth redaction utility for logging and customer-facing text.
 * <p>
 * This utility masks sensitive values that may accidentally appear in logs or
 * API responses, such as verification tokens, reset tokens, API keys, and other
 * credential-like strings. The authoritative fix is to never persist secrets in
 * these strings; this class provides a safety net for defence-in-depth.
 * <p>
 * Instances are never created — all methods are static. This class is thread-safe.
 */
public final class SensitiveDataRedactor {

    private SensitiveDataRedactor() {
        // private constructor — utility class, no instantiation
    }

    /**
     * Redacts sensitive values from the given input string.
     * <p>
     * Returns {@code null} for {@code null} input. Returns the input unchanged
     * when it is blank (empty or whitespace-only).
     * <p>
     * Redaction steps are applied in order:
     * <ol>
     *   <li>URL query parameter values for token-like parameters are masked.</li>
     *   <li>Labelled secrets (e.g. "Token: xyz", "token=abc") are masked.</li>
     *   <li>Unlabelled token runs (32+ chars from [A-Za-z0-9_-] with a digit,
     *       mixed case, or a {@code -}/{@code _}) are masked.</li>
     * </ol>
     * Any regex failure is handled conservatively — the method never throws.
     *
     * @param input the string to redact; may be {@code null}
     * @return {@code null} if input was {@code null}, the input unchanged if blank,
     *     otherwise the input with sensitive values masked
     */
    public static String redact(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return input;
        }

        String result = input;

        // 1. Mask URL query parameter values for token-like parameters
        result = maskUrlQueryParams(result);

        // 2. Mask labelled secrets in free text
        result = maskLabelledSecrets(result);

        // 3. Mask unlabelled token runs (32+ chars, token-like characteristics)
        result = maskUnlabelledTokenRuns(result);

        return result;
    }

    // -----------------------------------------------------------------
    // 1. URL query parameter masking
    // -----------------------------------------------------------------

    private static final Pattern URL_QUERY_PATTERN = Pattern.compile(
        "([?&])(token|code|resetToken|access_token|api_key|apikey|secret|key)=[^&\\s'\"]+",
        Pattern.CASE_INSENSITIVE
    );

    private static String maskUrlQueryParams(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        try {
            return URL_QUERY_PATTERN.matcher(input).replaceAll("$1$2=[REDACTED]");
        } catch (Exception e) {
            // conservative: return input with a single [REDACTED] marker
            return input.replaceFirst(".", "[REDACTED]");
        }
    }

    // -----------------------------------------------------------------
    // 2. Labelled secrets in free text
    // -----------------------------------------------------------------

    private static final Pattern LABELLED_SECRET_PATTERN = Pattern.compile(
        "(token|secret|api key)([:=]\\s*)[^\\s&]+",
        Pattern.CASE_INSENSITIVE
    );

    private static String maskLabelledSecrets(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        try {
            return LABELLED_SECRET_PATTERN.matcher(input).replaceAll("$1$2[REDACTED]");
        } catch (Exception e) {
            // conservative
            return input.replaceFirst(".", "[REDACTED]");
        }
    }

    // -----------------------------------------------------------------
    // 3. Unlabelled token runs (32+ chars, token-like characteristics)
    // -----------------------------------------------------------------

    /**
     * Returns {@code true} if the given string {@code s} (assumed to be 32+
     * characters from {[A-Za-z0-9_-]}) contains token-like characteristics:
     * at least one digit, or both uppercase and lowercase letters, or a {@code -}
     * or {@code _}.
     */
    private static boolean hasTokenCharacteristics(String s) {
        boolean hasDigit = false;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasHyphenOrUnderscore = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (c == '-' || c == '_') {
                hasHyphenOrUnderscore = true;
            }
        }

        // Must have at least one of: digit, mixed case (upper+lower), or hyphen/underscore
        return hasDigit || (hasUpper && hasLower) || hasHyphenOrUnderscore;
    }

    private static final Pattern UNLABELLED_TOKEN_PATTERN = Pattern.compile(
        "[A-Za-z0-9_-]{32,}"
    );

    private static String maskUnlabelledTokenRuns(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        try {
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            Matcher matcher = UNLABELLED_TOKEN_PATTERN.matcher(input);

            while (matcher.find()) {
                // Append text before this match
                sb.append(input, lastEnd, matcher.start());

                String match = matcher.group();
                if (hasTokenCharacteristics(match)) {
                    sb.append("[REDACTED]");
                } else {
                    sb.append(match);
                }

                lastEnd = matcher.end();
            }
            // Append remaining tail
            sb.append(input, lastEnd, input.length());

            return sb.toString();
        } catch (Exception e) {
            // conservative: fully mask the input
            return "[REDACTED]";
        }
    }
}