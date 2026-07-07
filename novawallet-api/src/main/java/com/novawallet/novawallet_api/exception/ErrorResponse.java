package com.novawallet.novawallet_api.exception;

import java.time.LocalDateTime;

public record ErrorResponse(

        int status,

        String code,

        String message,

        LocalDateTime timestamp,

        String path

) {}

