package com.novawallet.novawallet_api.exception;

/** HTTP 429: rate limit exceeded. */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
