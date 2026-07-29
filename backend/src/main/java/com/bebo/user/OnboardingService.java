package com.bebo.user;

import com.bebo.common.exception.BadRequestException;
import com.bebo.common.exception.NotFoundException;
import com.bebo.cycle.CycleRecord;
import com.bebo.cycle.CycleRecordRepository;
import com.bebo.cycle.CycleService;
import com.bebo.cycle.dto.CreateCycleRecordRequest;
import com.bebo.cycle.dto.CyclePredictionResponse;
import com.bebo.cycle.dto.UpdateCycleRecordRequest;
import com.bebo.notification.telegram.TelegramConnectionService;
import com.bebo.notification.telegram.dto.TelegramConnectionResponse;
import com.bebo.settings.CycleSettings;
import com.bebo.settings.CycleSettingsRepository;
import com.bebo.settings.SettingsService;
import com.bebo.settings.dto.UpdateSettingsRequest;
import com.bebo.user.dto.CompleteOnboardingRequest;
import com.bebo.user.dto.OnboardingCycleRequest;
import com.bebo.user.dto.OnboardingReminderRequest;
import com.bebo.user.dto.OnboardingStateResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final UserRepository userRepository;

  private final CycleRecordRepository cycleRecordRepository;

  private final CycleSettingsRepository cycleSettingsRepository;

  private final CycleService cycleService;

  private final SettingsService settingsService;

  private final TelegramConnectionService telegramConnectionService;

  public OnboardingService(
      UserRepository userRepository,
      CycleRecordRepository cycleRecordRepository,
      CycleSettingsRepository cycleSettingsRepository,
      CycleService cycleService,
      SettingsService settingsService,
      TelegramConnectionService telegramConnectionService) {
    this.userRepository = userRepository;

    this.cycleRecordRepository = cycleRecordRepository;

    this.cycleSettingsRepository = cycleSettingsRepository;

    this.cycleService = cycleService;

    this.settingsService = settingsService;

    this.telegramConnectionService = telegramConnectionService;
  }

  @Transactional(readOnly = true)
  public OnboardingStateResponse getState(User currentUser) {
    User managedUser = requireManagedUser(currentUser);

    return buildState(managedUser);
  }

  @Transactional
  public OnboardingStateResponse start(User currentUser) {
    User managedUser = requireManagedUser(currentUser);

    managedUser.advanceOnboardingTo(OnboardingStep.CYCLE);

    return buildState(managedUser);
  }

  @Transactional
  public OnboardingStateResponse saveCycle(User currentUser, OnboardingCycleRequest request) {
    User managedUser = requireManagedUser(currentUser);

    if (managedUser.isOnboardingCompleted()) {
      return buildState(managedUser);
    }

    List<CycleRecord> records =
        cycleRecordRepository.findAllByUser_IdOrderByStartDateDesc(managedUser.getId());

    if (records.isEmpty()) {
      cycleService.create(managedUser, new CreateCycleRecordRequest(request.startDate()));
    } else {
      CycleRecord mostRecentRecord = records.getFirst();

      if (!mostRecentRecord.getStartDate().equals(request.startDate())) {
        cycleService.update(
            managedUser,
            mostRecentRecord.getId(),
            new UpdateCycleRecordRequest(request.startDate()));
      }
    }

    CycleSettings settings = requireSettings(managedUser);

    settings.updateDefaultCycleLength(request.defaultCycleLength());

    managedUser.advanceOnboardingTo(OnboardingStep.REMINDER);

    return buildState(managedUser);
  }

  @Transactional
  public OnboardingStateResponse saveReminder(User currentUser, OnboardingReminderRequest request) {
    User managedUser = requireManagedUser(currentUser);

    if (managedUser.isOnboardingCompleted()) {
      return buildState(managedUser);
    }

    CycleSettings currentSettings = requireSettings(managedUser);

    settingsService.updateSettings(
        managedUser,
        new UpdateSettingsRequest(
            currentSettings.getDefaultCycleLength(),
            request.reminderDaysBefore(),
            request.notificationTime(),
            request.timezone()));

    managedUser.advanceOnboardingTo(OnboardingStep.TELEGRAM);

    return buildState(managedUser);
  }

  @Transactional
  public OnboardingStateResponse complete(User currentUser, CompleteOnboardingRequest request) {
    User managedUser = requireManagedUser(currentUser);

    if (managedUser.isOnboardingCompleted()) {
      return buildState(managedUser);
    }

    boolean cycleRecordMissing =
        cycleRecordRepository.findAllByUser_IdOrderByStartDateDesc(managedUser.getId()).isEmpty();

    if (cycleRecordMissing) {
      throw new BadRequestException("A cycle record is required " + "before completing onboarding");
    }

    TelegramConnectionResponse telegramConnection =
        telegramConnectionService.getStatus(managedUser);

    if (!request.skipTelegram() && !telegramConnection.connected()) {
      throw new BadRequestException(
          "Connect Telegram or choose " + "to skip it before completing onboarding");
    }

    managedUser.completeOnboarding(Instant.now());

    return buildState(managedUser);
  }

  private OnboardingStateResponse buildState(User user) {
    CycleSettings settings = requireSettings(user);

    List<CycleRecord> records =
        cycleRecordRepository.findAllByUser_IdOrderByStartDateDesc(user.getId());

    CycleRecord mostRecentRecord = records.isEmpty() ? null : records.getFirst();

    CyclePredictionResponse prediction =
        mostRecentRecord == null ? null : cycleService.getPrediction(user);

    TelegramConnectionResponse telegramConnection = telegramConnectionService.getStatus(user);

    return new OnboardingStateResponse(
        user.getOnboardingStep(),
        user.getOnboardingCompletedAt(),
        mostRecentRecord == null ? null : mostRecentRecord.getStartDate(),
        settings.getDefaultCycleLength(),
        settings.getReminderDaysBefore(),
        settings.getNotificationTime().format(TIME_FORMATTER),
        user.getTimezone(),
        telegramConnection.status(),
        telegramConnection.connected(),
        telegramConnection.telegramUsername(),
        prediction == null ? null : prediction.expectedNextPeriodDate(),
        prediction == null ? null : prediction.reminderDate());
  }

  private User requireManagedUser(User currentUser) {
    return userRepository
        .findById(currentUser.getId())
        .filter(User::isActive)
        .orElseThrow(() -> new NotFoundException("User was not found"));
  }

  private CycleSettings requireSettings(User user) {
    return cycleSettingsRepository
        .findByUser_Id(user.getId())
        .orElseThrow(() -> new NotFoundException("Cycle settings were not found"));
  }
}
