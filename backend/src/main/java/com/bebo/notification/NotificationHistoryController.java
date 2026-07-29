package com.bebo.notification;

import com.bebo.notification.dto.NotificationHistoryPageResponse;
import com.bebo.security.CurrentUserService;
import com.bebo.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationHistoryController {

  private final CurrentUserService currentUserService;

  private final NotificationHistoryService notificationHistoryService;

  public NotificationHistoryController(
      CurrentUserService currentUserService,
      NotificationHistoryService notificationHistoryService) {
    this.currentUserService = currentUserService;

    this.notificationHistoryService = notificationHistoryService;
  }

  @GetMapping("/history")
  public NotificationHistoryPageResponse getHistory(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    User currentUser = currentUserService.requireCurrentUser(authentication);

    return notificationHistoryService.getHistory(currentUser, page, size);
  }
}
