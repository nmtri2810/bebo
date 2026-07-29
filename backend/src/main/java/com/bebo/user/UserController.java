package com.bebo.user;

import com.bebo.security.CurrentUserService;
import com.bebo.user.dto.CompleteOnboardingRequest;
import com.bebo.user.dto.OnboardingCycleRequest;
import com.bebo.user.dto.OnboardingReminderRequest;
import com.bebo.user.dto.OnboardingStateResponse;
import com.bebo.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final CurrentUserService currentUserService;

  private final OnboardingService onboardingService;

  public UserController(
      CurrentUserService currentUserService, OnboardingService onboardingService) {
    this.currentUserService = currentUserService;

    this.onboardingService = onboardingService;
  }

  @GetMapping("/me")
  public UserResponse getCurrentUser(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return UserResponse.from(user);
  }

  @GetMapping("/me/onboarding")
  public OnboardingStateResponse getOnboardingState(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return onboardingService.getState(user);
  }

  @PostMapping("/me/onboarding/start")
  public OnboardingStateResponse startOnboarding(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return onboardingService.start(user);
  }

  @PutMapping("/me/onboarding/cycle")
  public OnboardingStateResponse saveOnboardingCycle(
      Authentication authentication, @Valid @RequestBody OnboardingCycleRequest request) {
    User user = currentUserService.requireCurrentUser(authentication);

    return onboardingService.saveCycle(user, request);
  }

  @PutMapping("/me/onboarding/reminder")
  public OnboardingStateResponse saveOnboardingReminder(
      Authentication authentication, @Valid @RequestBody OnboardingReminderRequest request) {
    User user = currentUserService.requireCurrentUser(authentication);

    return onboardingService.saveReminder(user, request);
  }

  @PostMapping("/me/onboarding/complete")
  public OnboardingStateResponse completeOnboarding(
      Authentication authentication, @RequestBody CompleteOnboardingRequest request) {
    User user = currentUserService.requireCurrentUser(authentication);

    return onboardingService.complete(user, request);
  }
}
