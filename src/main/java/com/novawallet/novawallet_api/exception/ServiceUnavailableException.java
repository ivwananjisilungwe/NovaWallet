package com.novawallet.novawallet_api.exception;

/** HTTP 503: a required downstream capability is unavailable or disabled. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
