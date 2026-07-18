package com.bebo.user.dto;

import com.bebo.user.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id, String email, String timezone, String status, Instant createdAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getTimezone(),
        user.getStatus().name(),
        user.getCreatedAt());
  }
}
