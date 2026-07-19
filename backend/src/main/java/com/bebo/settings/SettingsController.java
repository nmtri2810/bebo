package com.bebo.settings;

import com.bebo.security.CurrentUserService;
import com.bebo.settings.dto.SettingsResponse;
import com.bebo.settings.dto.UpdateSettingsRequest;
import com.bebo.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

  private final CurrentUserService currentUserService;

  private final SettingsService settingsService;

  public SettingsController(
      CurrentUserService currentUserService, SettingsService settingsService) {
    this.currentUserService = currentUserService;

    this.settingsService = settingsService;
  }

  @GetMapping
  public SettingsResponse getSettings(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return settingsService.getSettings(user);
  }

  @PutMapping
  public SettingsResponse updateSettings(
      Authentication authentication, @Valid @RequestBody UpdateSettingsRequest request) {

    User user = currentUserService.requireCurrentUser(authentication);

    return settingsService.updateSettings(user, request);
  }
}
