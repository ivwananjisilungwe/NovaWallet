package com.novawallet.novawallet_api.exception;

/** HTTP 404: resource not found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}