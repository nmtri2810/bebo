package com.bebo.security;

import com.bebo.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    ApiError error =
        ApiError.create(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            "You do not have permission to access this resource",
            request.getRequestURI());

    response.setStatus(HttpStatus.FORBIDDEN.value());

    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    objectMapper.writeValue(response.getOutputStream(), error);
  }
}
