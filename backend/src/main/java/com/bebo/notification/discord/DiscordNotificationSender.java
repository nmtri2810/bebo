package com.bebo.notification.discord;

import com.bebo.notification.ChannelType;
import com.bebo.notification.delivery.NotificationDeliveryRequest;
import com.bebo.notification.delivery.NotificationSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bebo.discord", name = "enabled", havingValue = "true")
public class DiscordNotificationSender implements NotificationSender {

  private final DiscordBotClient discordBotClient;

  public DiscordNotificationSender(DiscordBotClient discordBotClient) {
    this.discordBotClient = discordBotClient;
  }

  @Override
  public ChannelType supportedChannel() {
    return ChannelType.DISCORD;
  }

  @Override
  public void send(NotificationDeliveryRequest request) {
    discordBotClient.sendDirectMessage(request.recipientId(), request.messageBody());
  }
}
