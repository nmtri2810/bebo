package com.bebo.notification.delivery;

import com.bebo.notification.ChannelType;

public interface NotificationSender {

  ChannelType supportedChannel();

  void send(NotificationDeliveryRequest request);
}
