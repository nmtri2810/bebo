package com.bebo.notification.telegram;

import com.bebo.notification.telegram.TelegramBotClient.TelegramIncomingMessage;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class TelegramUpdatePoller {

  private static final Logger log = LoggerFactory.getLogger(TelegramUpdatePoller.class);

  private final TelegramBotClient telegramBotClient;

  private final TelegramUpdateProcessor updateProcessor;

  private final AtomicLong nextOffset = new AtomicLong(0);

  public TelegramUpdatePoller(
      TelegramBotClient telegramBotClient, TelegramUpdateProcessor updateProcessor) {
    this.telegramBotClient = telegramBotClient;

    this.updateProcessor = updateProcessor;
  }

  @Scheduled(fixedDelayString = "${bebo.telegram.poll-delay-ms:3000}")
  public void poll() {
    try {
      for (TelegramIncomingMessage update : telegramBotClient.getUpdates(nextOffset.get())) {
        try {
          updateProcessor.process(update);
        } catch (RuntimeException exception) {
          log.warn("Could not process Telegram " + "update {}", update.updateId(), exception);
        } finally {
          nextOffset.updateAndGet(current -> Math.max(current, update.updateId() + 1));
        }
      }
    } catch (RuntimeException exception) {
      log.warn("Could not poll Telegram updates", exception);
    }
  }
}
