package com.bebo.auth;

import com.bebo.auth.dto.AuthResponse;
import com.bebo.auth.dto.LoginRequest;
import com.bebo.auth.dto.RegisterRequest;
import com.bebo.common.exception.BadRequestException;
import com.bebo.common.exception.ConflictException;
import com.bebo.security.JwtService;
import com.bebo.settings.CycleSettings;
import com.bebo.settings.CycleSettingsRepository;
import com.bebo.user.User;
import com.bebo.user.UserRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

  private final UserRepository userRepository;
  private final CycleSettingsRepository cycleSettingsRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      CycleSettingsRepository cycleSettingsRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.cycleSettingsRepository = cycleSettingsRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase();

    if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new ConflictException("Email is already registered");
    }

    String timezone = resolveTimezone(request.timezone());

    User user = User.create(normalizedEmail, passwordEncoder.encode(request.password()), timezone);

    userRepository.save(user);

    CycleSettings cycleSettings = CycleSettings.createDefault(user);

    cycleSettingsRepository.save(cycleSettings);

    return createResponse(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmailIgnoreCase(request.email().trim())
            .filter(User::isActive)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

    if (!passwordMatches) {
      throw new BadCredentialsException("Invalid credentials");
    }

    return createResponse(user);
  }

  private AuthResponse createResponse(User user) {
    JwtService.TokenResult result = jwtService.createAccessToken(user);

    return new AuthResponse(
        result.value(),
        "Bearer",
        result.expiresAt(),
        user.getId(),
        user.getEmail(),
        user.getTimezone());
  }

  private String resolveTimezone(String requestedTimezone) {
    String timezone =
        requestedTimezone == null || requestedTimezone.isBlank()
            ? DEFAULT_TIMEZONE
            : requestedTimezone.trim();

    try {
      ZoneId.of(timezone);
    } catch (DateTimeException exception) {
      throw new BadRequestException("Timezone is invalid: " + timezone, exception);
    }

    return timezone;
  }
}
