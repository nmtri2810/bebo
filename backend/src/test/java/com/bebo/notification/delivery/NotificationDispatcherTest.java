package com.bebo.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bebo.notification.ChannelType;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationDispatcherTest {

  @Test
  void supportsAndDispatchesToConfiguredSender() {
    NotificationSender telegramSender = mock(NotificationSender.class);

    when(telegramSender.supportedChannel()).thenReturn(ChannelType.TELEGRAM);

    NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(telegramSender));

    NotificationDeliveryRequest request = new NotificationDeliveryRequest("123456789", "message");

    assertThat(dispatcher.supports(ChannelType.TELEGRAM)).isTrue();

    dispatcher.send(ChannelType.TELEGRAM, request);

    verify(telegramSender).send(request);
  }

  @Test
  void rejectsUnsupportedChannel() {
    NotificationDispatcher dispatcher = new NotificationDispatcher(List.of());

    assertThat(dispatcher.supports(ChannelType.TELEGRAM)).isFalse();

    assertThatThrownBy(
            () ->
                dispatcher.send(
                    ChannelType.TELEGRAM, new NotificationDeliveryRequest("123456789", "message")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No notification sender is configured for TELEGRAM");
  }

  @Test
  void rejectsDuplicateSendersForSameChannel() {
    NotificationSender firstSender = mock(NotificationSender.class);

    NotificationSender secondSender = mock(NotificationSender.class);

    when(firstSender.supportedChannel()).thenReturn(ChannelType.TELEGRAM);

    when(secondSender.supportedChannel()).thenReturn(ChannelType.TELEGRAM);

    assertThatThrownBy(() -> new NotificationDispatcher(List.of(firstSender, secondSender)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Multiple notification senders are configured for TELEGRAM");
  }
}
