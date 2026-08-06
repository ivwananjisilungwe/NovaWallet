package com.novawallet.novawallet_api.exception;

/** HTTP 403: access denied. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
