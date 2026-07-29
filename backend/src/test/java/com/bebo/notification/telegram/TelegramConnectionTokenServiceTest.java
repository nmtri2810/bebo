package com.bebo.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramConnectionTokenServiceTest {

  private final TelegramConnectionTokenService tokenService = new TelegramConnectionTokenService();

  @Test
  void issueCreatesUrlSafeRawTokenAndHash() {
    TelegramConnectionTokenService.IssuedToken token = tokenService.issue();

    assertThat(token.rawToken()).matches("[A-Za-z0-9_-]+");
    assertThat(token.rawToken()).doesNotContain("=");
    assertThat(token.tokenHash()).hasSize(64);
    assertThat(token.tokenHash()).matches("[0-9a-f]+");
    assertThat(token.tokenHash()).isEqualTo(tokenService.hash(token.rawToken()));
  }

  @Test
  void hashIsDeterministicSha256Hex() {
    String firstHash = tokenService.hash("raw-token");
    String secondHash = tokenService.hash("raw-token");

    assertThat(firstHash).isEqualTo(secondHash);
    assertThat(firstHash).hasSize(64);
    assertThat(firstHash).matches("[0-9a-f]+");
  }
}
