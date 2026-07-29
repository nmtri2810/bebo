package com.bebo.notification.telegram;

import com.bebo.notification.telegram.dto.TelegramConnectLinkResponse;
import com.bebo.notification.telegram.dto.TelegramConnectionResponse;
import com.bebo.notification.telegram.dto.TelegramTestResponse;
import com.bebo.security.CurrentUserService;
import com.bebo.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification-channels/telegram")
public class TelegramConnectionController {

  private final CurrentUserService currentUserService;
  private final TelegramConnectionService telegramConnectionService;

  public TelegramConnectionController(
      CurrentUserService currentUserService, TelegramConnectionService telegramConnectionService) {
    this.currentUserService = currentUserService;
    this.telegramConnectionService = telegramConnectionService;
  }

  @GetMapping
  public TelegramConnectionResponse get(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return telegramConnectionService.getStatus(user);
  }

  @PostMapping("/connect")
  public TelegramConnectLinkResponse connect(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return telegramConnectionService.beginConnection(user);
  }

  @PostMapping("/test")
  public TelegramTestResponse sendTestMessage(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return telegramConnectionService.sendTestMessage(user);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disconnect(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    telegramConnectionService.disconnect(user);
  }
}
