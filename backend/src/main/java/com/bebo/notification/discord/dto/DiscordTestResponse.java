package com.bebo.notification.discord.dto;

import java.time.Instant;

public record DiscordTestResponse(boolean sent, Instant sentAt) {}
