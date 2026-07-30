package com.bebo.notification.discord;

import com.bebo.notification.discord.DiscordConnectionService.ConnectionAttempt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/public/notification-channels/discord")
public class DiscordOAuthCallbackController {

  private final DiscordConnectionService discordConnectionService;

  private final DiscordProperties properties;

  public DiscordOAuthCallbackController(
      DiscordConnectionService discordConnectionService, DiscordProperties properties) {
    this.discordConnectionService = discordConnectionService;

    this.properties = properties;
  }

  @GetMapping("/callback")
  public RedirectView callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error) {
    ConnectionAttempt result = discordConnectionService.completeConnection(code, state, error);

    String redirectUrl =
        UriComponentsBuilder.fromUriString(properties.getFrontendRedirectUrl())
            .queryParam("discord", result.queryValue())
            .build()
            .encode()
            .toUriString();

    RedirectView redirectView = new RedirectView(redirectUrl);

    redirectView.setExposeModelAttributes(false);

    return redirectView;
  }
}
