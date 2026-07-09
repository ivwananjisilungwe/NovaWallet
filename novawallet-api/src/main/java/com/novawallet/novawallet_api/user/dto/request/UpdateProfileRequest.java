package com.novawallet.novawallet_api.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        String firstName,

        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        String lastName,

        @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid (e.g., +260971234567)")
        String phone
) {}
