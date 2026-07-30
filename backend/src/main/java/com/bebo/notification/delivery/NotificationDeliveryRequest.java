package com.bebo.notification.delivery;

import java.util.Objects;

public record NotificationDeliveryRequest(String recipientId, String messageBody) {

  public NotificationDeliveryRequest {
    recipientId = requireText(recipientId, "Recipient ID");

    messageBody = requireText(messageBody, "Message body");
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");

    String normalized = value.trim();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return normalized;
  }
}
