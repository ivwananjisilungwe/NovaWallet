package com.novawallet.novawallet_api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard API response wrapper")
public record ApiResponse<T>(
        @Schema(description = "Indicates if the request was successful", example = "true")
        boolean success,

        @Schema(description = "Response payload")
        T data,

        @Schema(description = "Response message", example = "Operation completed successfully")
        String message,

        @Schema(description = "Response timestamp")
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
                true,
                data,
                message,
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
                false,
                null,
                message,
                LocalDateTime.now()
        );
    }
}
