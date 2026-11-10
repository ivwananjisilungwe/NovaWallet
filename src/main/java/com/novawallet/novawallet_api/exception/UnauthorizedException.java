package com.novawallet.novawallet_api.exception;

/** HTTP 401: authentication failed. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
