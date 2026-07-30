package com.bebo.notification.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bebo.cycle.CycleRecordRepository;
import com.bebo.cycle.CycleRecordRepository.CycleStartProjection;
import com.bebo.settings.CycleSettings;
import com.bebo.settings.CycleSettingsRepository;
import com.bebo.user.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CycleReminderPlanServiceTest {

  @Mock private CycleRecordRepository cycleRecordRepository;

  @Mock private CycleSettingsRepository cycleSettingsRepository;

  private ReminderProperties reminderProperties;

  private CycleReminderPlanService service;

  @BeforeEach
  void setUp() {
    reminderProperties = new ReminderProperties();

    reminderProperties.setMaxOverdueDays(14);

    service =
        new CycleReminderPlanService(
            cycleRecordRepository, cycleSettingsRepository, reminderProperties);
  }

  @Test
  void createPlanBuildsDailyReminderWindow() {
    UUID userId = UUID.randomUUID();

    UUID latestRecordId = UUID.randomUUID();

    User user = userWithId(userId);

    CycleSettings settings = CycleSettings.createDefault(user);

    settings.update(28, 3, LocalTime.of(8, 0));

    CycleStartProjection latest = projection(latestRecordId, LocalDate.of(2026, 7, 5));

    when(cycleSettingsRepository.findByUser_Id(userId)).thenReturn(Optional.of(settings));

    when(cycleRecordRepository.findRecentCycleStarts(userId, PageRequest.of(0, 7)))
        .thenReturn(List.of(latest));

    CycleReminderPlan plan = service.createPlan(user).orElseThrow();

    assertThat(plan.latestCycleRecordId()).isEqualTo(latestRecordId);

    assertThat(plan.predictedPeriodDate()).isEqualTo(LocalDate.of(2026, 8, 2));

    assertThat(plan.reminderStartDate()).isEqualTo(LocalDate.of(2026, 7, 30));

    assertThat(plan.reminderEndDate()).isEqualTo(LocalDate.of(2026, 8, 16));

    assertThat(plan.notificationTime()).isEqualTo(LocalTime.of(8, 0));
  }

  @Test
  void createPlanReturnsEmptyWithoutSettings() {
    User user = userWithId(UUID.randomUUID());

    when(cycleSettingsRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

    assertThat(service.createPlan(user)).isEmpty();
  }

  @Test
  void createPlanReturnsEmptyWithoutCycleRecords() {
    User user = userWithId(UUID.randomUUID());

    CycleSettings settings = CycleSettings.createDefault(user);

    when(cycleSettingsRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(settings));

    when(cycleRecordRepository.findRecentCycleStarts(user.getId(), PageRequest.of(0, 7)))
        .thenReturn(List.of());

    assertThat(service.createPlan(user)).isEmpty();
  }

  private CycleStartProjection projection(UUID id, LocalDate startDate) {
    CycleStartProjection projection = mock(CycleStartProjection.class);

    when(projection.getId()).thenReturn(id);

    when(projection.getStartDate()).thenReturn(startDate);

    return projection;
  }

  private User userWithId(UUID id) {
    User user = User.create("test-" + id + "@example.com", "{noop}secret", "UTC");

    ReflectionTestUtils.setField(user, "id", id);

    return user;
  }
}
