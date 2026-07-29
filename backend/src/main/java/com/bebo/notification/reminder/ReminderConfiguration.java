package com.bebo.notification.reminder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ReminderProperties.class)
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class ReminderConfiguration {}
