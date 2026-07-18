package com.bebo.security;

import com.bebo.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    ApiError error =
        ApiError.create(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "Authentication is required or the access token is invalid",
            request.getRequestURI());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());

    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    objectMapper.writeValue(response.getOutputStream(), error);
  }
}
