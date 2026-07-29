package com.bebo.notification.reminder;

import com.bebo.cycle.CyclePredictionCalculator;
import com.bebo.cycle.CycleRecordRepository;
import com.bebo.cycle.CycleRecordRepository.CycleStartProjection;
import com.bebo.settings.CycleSettings;
import com.bebo.settings.CycleSettingsRepository;
import com.bebo.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleReminderPlanService {

  private static final int MAX_RECENT_RECORDS = 7;

  private final CycleRecordRepository cycleRecordRepository;

  private final CycleSettingsRepository cycleSettingsRepository;

  public CycleReminderPlanService(
      CycleRecordRepository cycleRecordRepository,
      CycleSettingsRepository cycleSettingsRepository) {
    this.cycleRecordRepository = cycleRecordRepository;

    this.cycleSettingsRepository = cycleSettingsRepository;
  }

  @Transactional(readOnly = true)
  public Optional<CycleReminderPlan> createPlan(User user) {
    CycleSettings settings = cycleSettingsRepository.findByUser_Id(user.getId()).orElse(null);

    if (settings == null) {
      return Optional.empty();
    }

    List<CycleStartProjection> recentStarts =
        cycleRecordRepository.findRecentCycleStarts(
            user.getId(), PageRequest.of(0, MAX_RECENT_RECORDS));

    if (recentStarts.isEmpty()) {
      return Optional.empty();
    }

    List<LocalDate> startDates =
        recentStarts.stream().map(CycleStartProjection::getStartDate).toList();

    CyclePredictionCalculator.Calculation calculation =
        CyclePredictionCalculator.calculateAverage(startDates, settings.getDefaultCycleLength());

    CycleStartProjection latestRecord = recentStarts.getFirst();

    LocalDate predictedPeriodDate =
        latestRecord.getStartDate().plusDays(calculation.averageCycleLength());

    LocalDate reminderDate = predictedPeriodDate.minusDays(settings.getReminderDaysBefore());

    ZoneId userZone = ZoneId.of(user.getTimezone());

    ZonedDateTime localScheduledTime =
        ZonedDateTime.of(reminderDate, settings.getNotificationTime(), userZone);

    Instant scheduledFor = localScheduledTime.toInstant();

    return Optional.of(
        new CycleReminderPlan(
            latestRecord.getId(),
            predictedPeriodDate,
            reminderDate,
            scheduledFor,
            settings.getReminderDaysBefore()));
  }
}
