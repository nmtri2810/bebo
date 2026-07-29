package com.bebo.notification.telegram.dto;

import com.bebo.notification.NotificationChannelStatus;
import java.time.Instant;

public record TelegramConnectLinkResponse(
    NotificationChannelStatus status, String deepLink, Instant expiresAt) {}
