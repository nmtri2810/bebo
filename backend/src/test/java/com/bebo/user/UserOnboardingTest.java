package com.bebo.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserOnboardingTest {

  @Test
  void newUserStartsAtWelcomeAndCanCompleteOnboarding() {
    User user = User.create("test@example.com", "{noop}secret", "UTC");

    assertThat(user.getOnboardingStep()).isEqualTo(OnboardingStep.WELCOME);

    assertThat(user.getOnboardingCompletedAt()).isNull();

    user.advanceOnboardingTo(OnboardingStep.CYCLE);

    user.advanceOnboardingTo(OnboardingStep.REMINDER);

    user.advanceOnboardingTo(OnboardingStep.CHANNELS);

    Instant completedAt = Instant.now();

    user.completeOnboarding(completedAt);

    assertThat(user.getOnboardingStep()).isEqualTo(OnboardingStep.COMPLETED);

    assertThat(user.getOnboardingCompletedAt()).isEqualTo(completedAt);

    assertThat(user.isOnboardingCompleted()).isTrue();
  }
}
