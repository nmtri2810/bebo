package com.bebo.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bebo.cors")
public record CorsProperties(List<String> allowedOrigins) {}
