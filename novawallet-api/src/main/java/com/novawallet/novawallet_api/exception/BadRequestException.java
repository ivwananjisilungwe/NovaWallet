package com.novawallet.novawallet_api.exception;

/** HTTP 400: invalid client request. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
