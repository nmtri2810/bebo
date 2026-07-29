package com.bebo.notification.reminder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetryPolicy {

  private final ReminderProperties properties;

  public NotificationRetryPolicy(ReminderProperties properties) {
    this.properties = properties;
  }

  public Optional<Instant> nextRetryAtAfterFailure(int failedAttemptCount, Instant failedAt) {
    Duration delay = getRetryDelay(failedAttemptCount);

    if (delay == null) {
      return Optional.empty();
    }

    return Optional.of(failedAt.plus(delay));
  }

  private Duration getRetryDelay(int failedAttemptCount) {
    return switch (failedAttemptCount) {
      /*
       * Initial attempt failed.
       * Schedule retry number one.
       */
      case 1 -> properties.getFirstRetryDelay();

      /*
       * Retry number one failed.
       * Schedule retry number two.
       */
      case 2 -> properties.getSecondRetryDelay();

      /*
       * Retry number two failed.
       * Schedule retry number three.
       */
      case 3 -> properties.getThirdRetryDelay();

      /*
       * Retry number three failed.
       * Stop retrying.
       */
      default -> null;
    };
  }
}
