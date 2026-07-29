package com.bebo.notification.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class TelegramConnectionTokenService {

  private static final int TOKEN_BYTES = 32;

  private final SecureRandom secureRandom = new SecureRandom();

  public IssuedToken issue() {
    byte[] bytes = new byte[TOKEN_BYTES];

    secureRandom.nextBytes(bytes);

    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    return new IssuedToken(rawToken, hash(rawToken));
  }

  public String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  public record IssuedToken(String rawToken, String tokenHash) {}
}
