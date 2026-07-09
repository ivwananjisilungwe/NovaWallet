package com.novawallet.novawallet_api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        int status,

        String code,

        String message,

        LocalDateTime timestamp,

        String path,

        Map<String, String> errors

) {

    public ErrorResponse(int status, String code, String message, LocalDateTime timestamp, String path) {
        this(status, code, message, timestamp, path, null);
    }
}

