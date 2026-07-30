package com.bebo.notification.telegram;

import com.bebo.notification.telegram.TelegramBotClient.TelegramIncomingMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class TelegramUpdateProcessor {

  private static final Logger log = LoggerFactory.getLogger(TelegramUpdateProcessor.class);

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
        log.info("Telegram start command did not include a valid bebo connection token");

        telegramBotClient.sendMessage(
            incoming.chatId(),
            "Open Telegram from your " + "bebo Settings page " + "to connect your account.");
      }

      return;
    }

    String rawToken = matcher.group(1);

    TelegramConnectionService.ConnectionAttempt result =
        connectionService.completeConnection(rawToken, incoming.chatId(), incoming.username());

    log.info("Telegram connection update processed with result {}", result);

    switch (result) {
      case CONNECTED ->
          telegramBotClient.sendMessage(
              incoming.chatId(), "Telegram is now connected " + "to your bebo account.");

      case ALREADY_LINKED ->
          telegramBotClient.sendMessage(
              incoming.chatId(),
              """
              This Telegram account is already connected to another bebo account.

              Disconnect Telegram from the other bebo account before trying again.
              """
                  .strip());

      case EXPIRED ->
          telegramBotClient.sendMessage(
              incoming.chatId(),
              "This connection link has expired. " + "Generate a new link in bebo.");

      case INVALID ->
          telegramBotClient.sendMessage(
              incoming.chatId(), "This connection link is invalid " + "or has already been used.");
    }
  }
}
