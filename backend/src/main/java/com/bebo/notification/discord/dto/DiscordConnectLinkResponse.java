package com.bebo.notification.discord.dto;

import com.bebo.notification.NotificationChannelStatus;
import java.time.Instant;

public record DiscordConnectLinkResponse(
    NotificationChannelStatus status, String authorizationUrl, Instant expiresAt) {}
