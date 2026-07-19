package com.bebo.settings;

import com.bebo.common.exception.BadRequestException;
import com.bebo.common.exception.NotFoundException;
import com.bebo.settings.dto.SettingsResponse;
import com.bebo.settings.dto.UpdateSettingsRequest;
import com.bebo.user.User;
import com.bebo.user.UserRepository;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final CycleSettingsRepository cycleSettingsRepository;

  private final UserRepository userRepository;

  public SettingsService(
      CycleSettingsRepository cycleSettingsRepository, UserRepository userRepository) {
    this.cycleSettingsRepository = cycleSettingsRepository;

    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public SettingsResponse getSettings(User currentUser) {
    CycleSettings settings = requireSettings(currentUser.getId());

    return SettingsResponse.from(settings, currentUser);
  }

  @Transactional
  public SettingsResponse updateSettings(User currentUser, UpdateSettingsRequest request) {
    /*
     * Reload user bên trong transaction để entity
     * chắc chắn đang được Hibernate quản lý.
     */
    User managedUser =
        userRepository
            .findById(currentUser.getId())
            .filter(User::isActive)
            .orElseThrow(() -> new NotFoundException("User was not found"));

    CycleSettings settings = requireSettings(managedUser.getId());

    String timezone = validateTimezone(request.timezone());

    LocalTime notificationTime = parseNotificationTime(request.notificationTime());

    settings.update(request.defaultCycleLength(), request.reminderDaysBefore(), notificationTime);

    managedUser.updateTimezone(timezone);

    /*
     * settings và managedUser đều đang được quản lý.
     * Hibernate sẽ dirty-check khi transaction kết thúc.
     */
    return SettingsResponse.from(settings, managedUser);
  }

  private CycleSettings requireSettings(UUID userId) {
    return cycleSettingsRepository
        .findByUser_Id(userId)
        .orElseThrow(() -> new NotFoundException("Cycle settings were not found"));
  }

  private String validateTimezone(String requestedTimezone) {
    String timezone = requestedTimezone.trim();

    try {
      ZoneId.of(timezone);
    } catch (DateTimeException exception) {
      throw new BadRequestException("Timezone is invalid: " + timezone, exception);
    }

    return timezone;
  }

  private LocalTime parseNotificationTime(String value) {
    try {
      return LocalTime.parse(value, TIME_FORMATTER);
    } catch (DateTimeParseException exception) {
      throw new BadRequestException("Notification time must use HH:mm format", exception);
    }
  }
}
