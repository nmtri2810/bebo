package com.bebo.notification.discord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class DiscordOAuthStateService {

  private static final int STATE_BYTES = 32;

  private final SecureRandom secureRandom = new SecureRandom();

  public IssuedState issue() {
    byte[] bytes = new byte[STATE_BYTES];

    secureRandom.nextBytes(bytes);

    String rawState = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    return new IssuedState(rawState, hash(rawState));
  }

  public String hash(String rawState) {
    if (rawState == null || rawState.isBlank()) {
      throw new IllegalArgumentException("Discord OAuth state must not be blank");
    }

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashed = digest.digest(rawState.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  public record IssuedState(String rawState, String stateHash) {}
}
