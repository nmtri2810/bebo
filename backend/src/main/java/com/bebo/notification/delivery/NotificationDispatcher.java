package com.bebo.notification.delivery;

import com.bebo.notification.ChannelType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatcher {

  private final Map<ChannelType, NotificationSender> senders;

  public NotificationDispatcher(List<NotificationSender> notificationSenders) {
    EnumMap<ChannelType, NotificationSender> senderMap = new EnumMap<>(ChannelType.class);

    for (NotificationSender sender : notificationSenders) {
      ChannelType channelType = sender.supportedChannel();

      NotificationSender existing = senderMap.put(channelType, sender);

      if (existing != null) {
        throw new IllegalStateException(
            "Multiple notification senders are configured for " + channelType);
      }
    }

    this.senders = Collections.unmodifiableMap(senderMap);
  }

  public boolean supports(ChannelType channelType) {
    return senders.containsKey(channelType);
  }

  public void send(ChannelType channelType, NotificationDeliveryRequest request) {
    NotificationSender sender = senders.get(channelType);

    if (sender == null) {
      throw new IllegalStateException("No notification sender is configured for " + channelType);
    }

    sender.send(request);
  }
}
