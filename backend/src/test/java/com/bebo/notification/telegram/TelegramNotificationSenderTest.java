package com.bebo.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.bebo.notification.ChannelType;
import com.bebo.notification.delivery.NotificationDeliveryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationSenderTest {

  @Mock private TelegramBotClient telegramBotClient;

  private TelegramNotificationSender sender;

  @BeforeEach
  void setUp() {
    sender = new TelegramNotificationSender(telegramBotClient);
  }

  @Test
  void supportsTelegram() {
    assertThat(sender.supportedChannel()).isEqualTo(ChannelType.TELEGRAM);
  }

  @Test
  void sendsMessageToTelegramChatId() {
    sender.send(new NotificationDeliveryRequest("123456789", "cycle reminder"));

    verify(telegramBotClient).sendMessage(123456789L, "cycle reminder");
  }

  @Test
  void rejectsInvalidTelegramRecipientId() {
    assertThatThrownBy(
            () -> sender.send(new NotificationDeliveryRequest("not-a-chat-id", "cycle reminder")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Telegram recipient ID is invalid");
  }
}
