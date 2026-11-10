package com.novawallet.novawallet_api.exception;

/** HTTP 409: resource already exists. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
