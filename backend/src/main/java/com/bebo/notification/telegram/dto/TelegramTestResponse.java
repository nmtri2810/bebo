package com.bebo.notification.telegram.dto;

import java.time.Instant;

public record TelegramTestResponse(boolean sent, Instant sentAt) {}
