package com.bebo.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    UUID userId,
    String email,
    String timezone) {}
