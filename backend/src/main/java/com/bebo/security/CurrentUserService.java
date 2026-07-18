package com.bebo.security;

import com.bebo.common.exception.NotFoundException;
import com.bebo.user.User;
import com.bebo.user.UserRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

  private final UserRepository userRepository;

  public CurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User requireCurrentUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotFoundException("Authenticated user was not found");
    }

    UUID userId;

    try {
      /*
       * Resource Server dùng JWT claim "sub"
       * làm Authentication.getName().
       */
      userId = UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException exception) {
      throw new NotFoundException("Authenticated user is invalid");
    }

    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new NotFoundException("Authenticated user was not found"));
  }
}
