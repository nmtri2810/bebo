package com.bebo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
    @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters")
        String password,
    @Size(max = 100, message = "Timezone must not exceed 100 characters") String timezone) {}
