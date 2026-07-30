package com.bebo.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import com.bebo.notification.telegram.TelegramBotClient.TelegramIncomingMessage;
import com.bebo.user.User;
import com.bebo.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    properties = {
      "bebo.telegram.enabled=true",
      "bebo.telegram.bot-token=test-token",
      "bebo.telegram.bot-username=bebo_test_bot",
      "bebo.telegram.poll-delay-ms=3600000",
      "bebo.reminder.enabled=false"
    })
@Transactional
class TelegramConnectionIntegrationTest {

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private NotificationChannelRepository notificationChannelRepository;

  @Autowired private TelegramConnectionController telegramConnectionController;

  @Autowired private TelegramUpdateProcessor telegramUpdateProcessor;

  @Autowired private RecordingTelegramBotClient telegramBotClient;

  @Test
  void connectsTelegramFromApiDeepLinkAndDisconnectsThroughApi() throws Exception {
    User user =
        userRepository.saveAndFlush(
            User.create("telegram-it-" + System.nanoTime() + "@example.com", "{noop}secret", "UTC"));

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(telegramConnectionController).build();

    MvcResult connectResult =
        mockMvc
            .perform(
                post("/api/notification-channels/telegram/connect")
                    .principal(authenticationFor(user.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(
                jsonPath("$.deepLink")
                    .value(org.hamcrest.Matchers.startsWith("https://t.me/bebo_test_bot?start=")))
            .andReturn();

    String deepLink =
        objectMapper
            .readTree(connectResult.getResponse().getContentAsString())
            .path("deepLink")
            .asString();

    String rawToken = deepLink.substring(deepLink.indexOf("?start=") + "?start=".length());

    NotificationChannel pendingChannel =
        notificationChannelRepository
            .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
            .orElseThrow();

    assertThat(pendingChannel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.PENDING);

    telegramUpdateProcessor.process(
        new TelegramIncomingMessage(1001L, 987654321L, "bebo_user", "/start " + rawToken));

    NotificationChannel connectedChannel =
        notificationChannelRepository
            .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
            .orElseThrow();

    assertThat(connectedChannel.getConnectionStatus())
        .isEqualTo(NotificationChannelStatus.CONNECTED);

    assertThat(connectedChannel.isEnabled()).isTrue();

    assertThat(connectedChannel.getTelegramChatId()).isEqualTo(987654321L);

    assertThat(connectedChannel.getTelegramUsername()).isEqualTo("bebo_user");

    assertThat(connectedChannel.getConnectTokenHash()).isNull();

    assertThat(telegramBotClient.sentMessages())
        .contains(new SentTelegramMessage(987654321L, "Telegram is now connected to your bebo account."));

    mockMvc
        .perform(
            delete("/api/notification-channels/telegram")
                .principal(authenticationFor(user.getId())))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    NotificationChannel disconnectedChannel =
        notificationChannelRepository
            .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
            .orElseThrow();

    assertThat(disconnectedChannel.getConnectionStatus())
        .isEqualTo(NotificationChannelStatus.DISCONNECTED);

    assertThat(disconnectedChannel.isEnabled()).isFalse();

    assertThat(disconnectedChannel.getTelegramChatId()).isNull();

    assertThat(disconnectedChannel.getExternalRecipientId()).isNull();
  }

  private Authentication authenticationFor(UUID userId) {
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(userId.toString(), "n/a");

    authentication.setAuthenticated(true);

    return authentication;
  }

  @TestConfiguration
  static class TelegramConnectionIntegrationTestConfiguration {

    @Bean
    @Primary
    RecordingTelegramBotClient recordingTelegramBotClient(TelegramProperties properties) {
      return new RecordingTelegramBotClient(properties);
    }
  }

  static class RecordingTelegramBotClient extends TelegramBotClient {

    private final List<SentTelegramMessage> sentMessages = new ArrayList<>();

    RecordingTelegramBotClient(TelegramProperties properties) {
      super(properties);
    }

    @Override
    public List<TelegramIncomingMessage> getUpdates(long offset) {
      return List.of();
    }

    @Override
    public void sendMessage(long chatId, String text) {
      sentMessages.add(new SentTelegramMessage(chatId, text));
    }

    List<SentTelegramMessage> sentMessages() {
      return sentMessages;
    }
  }

  record SentTelegramMessage(long chatId, String text) {}
}
