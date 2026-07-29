package com.bebo.notification.telegram;

import com.bebo.notification.telegram.TelegramBotClient.TelegramIncomingMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class TelegramUpdateProcessor {

  private static final Pattern START_PATTERN =
      Pattern.compile("^/start(?:@\\w+)?\\s+" + "([A-Za-z0-9_-]{1,64})$");

  private final TelegramConnectionService connectionService;

  private final TelegramBotClient telegramBotClient;

  public TelegramUpdateProcessor(
      TelegramConnectionService connectionService, TelegramBotClient telegramBotClient) {

    this.connectionService = connectionService;

    this.telegramBotClient = telegramBotClient;
  }

  public void process(TelegramIncomingMessage incoming) {
    if (incoming.chatId() == 0 || incoming.text() == null) {
      return;
    }

    Matcher matcher = START_PATTERN.matcher(incoming.text().trim());

    if (!matcher.matches()) {
      if (incoming.text().startsWith("/start")) {
        telegramBotClient.sendMessage(
            incoming.chatId(),
            "Open Telegram from your " + "bebo Settings page " + "to connect your account.");
      }

      return;
    }

    String rawToken = matcher.group(1);

    TelegramConnectionService.ConnectionAttempt result =
        connectionService.completeConnection(rawToken, incoming.chatId(), incoming.username());

    switch (result) {
      case CONNECTED ->
          telegramBotClient.sendMessage(
              incoming.chatId(), "Telegram is now connected " + "to your bebo account.");

      case EXPIRED ->
          telegramBotClient.sendMessage(
              incoming.chatId(),
              "This connection link has " + "expired. Generate a " + "new link in bebo.");

      case INVALID ->
          telegramBotClient.sendMessage(
              incoming.chatId(),
              "This connection link is " + "invalid or has already " + "been used.");
    }
  }
}
