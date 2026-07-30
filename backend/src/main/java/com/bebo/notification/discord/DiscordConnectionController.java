package com.bebo.notification.discord;

import com.bebo.notification.discord.dto.DiscordConnectLinkResponse;
import com.bebo.notification.discord.dto.DiscordConnectionResponse;
import com.bebo.notification.discord.dto.DiscordTestResponse;
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
@RequestMapping("/api/notification-channels/discord")
public class DiscordConnectionController {

  private final CurrentUserService currentUserService;

  private final DiscordConnectionService discordConnectionService;

  public DiscordConnectionController(
      CurrentUserService currentUserService, DiscordConnectionService discordConnectionService) {
    this.currentUserService = currentUserService;

    this.discordConnectionService = discordConnectionService;
  }

  @GetMapping
  public DiscordConnectionResponse get(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return discordConnectionService.getStatus(user);
  }

  @PostMapping("/connect")
  public DiscordConnectLinkResponse connect(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return discordConnectionService.beginConnection(user);
  }

  @PostMapping("/test")
  public DiscordTestResponse sendTestMessage(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return discordConnectionService.sendTestMessage(user);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disconnect(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    discordConnectionService.disconnect(user);
  }
}
