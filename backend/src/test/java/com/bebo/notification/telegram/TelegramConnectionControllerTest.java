package com.bebo.notification.telegram;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bebo.security.CurrentUserService;
import com.bebo.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TelegramConnectionControllerTest {

  @Test
  void disconnectReturnsNoContent() throws Exception {
    CurrentUserService currentUserService = mock(CurrentUserService.class);
    TelegramConnectionService telegramConnectionService = mock(TelegramConnectionService.class);

    TelegramConnectionController controller =
        new TelegramConnectionController(currentUserService, telegramConnectionService);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    User user = User.create("test@example.com", "{noop}secret", "UTC");

    when(currentUserService.requireCurrentUser(any(Authentication.class))).thenReturn(user);

    Authentication authentication = new TestingAuthenticationToken("user-id", "token");

    mockMvc
        .perform(delete("/api/notification-channels/telegram").principal(authentication))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(telegramConnectionService).disconnect(user);
  }
}
