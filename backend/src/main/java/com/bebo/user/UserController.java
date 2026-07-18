package com.bebo.user;

import com.bebo.security.CurrentUserService;
import com.bebo.user.dto.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final CurrentUserService currentUserService;

  public UserController(CurrentUserService currentUserService) {
    this.currentUserService = currentUserService;
  }

  @GetMapping("/me")
  public UserResponse getCurrentUser(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return UserResponse.from(user);
  }
}
