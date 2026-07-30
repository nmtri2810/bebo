package com.bebo.notification.telegram;

import com.bebo.notification.ChannelType;
import com.bebo.notification.delivery.NotificationDeliveryRequest;
import com.bebo.notification.delivery.NotificationSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class TelegramNotificationSender implements NotificationSender {

  private final TelegramBotClient telegramBotClient;

  public TelegramNotificationSender(TelegramBotClient telegramBotClient) {
    this.telegramBotClient = telegramBotClient;
  }

  @Override
  public ChannelType supportedChannel() {
    return ChannelType.TELEGRAM;
  }

  @Override
  public void send(NotificationDeliveryRequest request) {
    long telegramChatId;

    try {
      telegramChatId = Long.parseLong(request.recipientId());
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Telegram recipient ID is invalid", exception);
    }

    telegramBotClient.sendMessage(telegramChatId, request.messageBody());
  }
}
