package com.bebo.user;

import com.bebo.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "app_users")
public class User extends BaseEntity {

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(nullable = false, length = 100)
  private String timezone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "onboarding_step", nullable = false, length = 20)
  private OnboardingStep onboardingStep;

  @Column(name = "onboarding_completed_at")
  private Instant onboardingCompletedAt;

  protected User() {}

  private User(String email, String passwordHash, String timezone) {
    this.email = normalizeEmail(email);
    this.passwordHash = passwordHash;
    this.timezone = validateTimezone(timezone);
    this.status = UserStatus.ACTIVE;
    this.onboardingStep = OnboardingStep.WELCOME;
  }

  public static User create(String email, String passwordHash, String timezone) {
    return new User(email, passwordHash, timezone);
  }

  public void updateTimezone(String timezone) {
    this.timezone = validateTimezone(timezone);
  }

  public void advanceOnboardingTo(OnboardingStep targetStep) {
    if (onboardingCompletedAt != null || targetStep == null) {
      return;
    }

    if (targetStep.ordinal() > onboardingStep.ordinal()) {
      this.onboardingStep = targetStep;
    }
  }

  public void completeOnboarding(Instant completedAt) {
    this.onboardingStep = OnboardingStep.COMPLETED;

    this.onboardingCompletedAt = completedAt;
  }

  public void disable() {
    this.status = UserStatus.DISABLED;
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private static String validateTimezone(String timezone) {
    ZoneId.of(timezone);

    return timezone;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getTimezone() {
    return timezone;
  }

  public UserStatus getStatus() {
    return status;
  }

  public OnboardingStep getOnboardingStep() {
    return onboardingStep;
  }

  public Instant getOnboardingCompletedAt() {
    return onboardingCompletedAt;
  }

  public boolean isOnboardingCompleted() {
    return onboardingStep == OnboardingStep.COMPLETED && onboardingCompletedAt != null;
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }
}
