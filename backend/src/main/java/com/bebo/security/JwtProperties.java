package com.bebo.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bebo.jwt")
public record JwtProperties(String issuer, Duration accessTokenTtl, String secret) {}
