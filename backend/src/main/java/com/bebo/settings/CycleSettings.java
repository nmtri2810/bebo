package com.bebo.settings;

import com.bebo.common.model.BaseEntity;
import com.bebo.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "cycle_settings")
public class CycleSettings extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "default_cycle_length", nullable = false)
  private int defaultCycleLength;

  @Column(name = "reminder_days_before", nullable = false)
  private int reminderDaysBefore;

  @Column(name = "notification_time", nullable = false)
  private LocalTime notificationTime;

  protected CycleSettings() {}

  private CycleSettings(User user) {
    this.user = user;
    this.defaultCycleLength = 28;
    this.reminderDaysBefore = 3;
    this.notificationTime = LocalTime.of(8, 0);
  }

  public static CycleSettings createDefault(User user) {
    return new CycleSettings(user);
  }

  public void update(int defaultCycleLength, int reminderDaysBefore, LocalTime notificationTime) {
    this.defaultCycleLength = defaultCycleLength;
    this.reminderDaysBefore = reminderDaysBefore;
    this.notificationTime = notificationTime;
  }

  public User getUser() {
    return user;
  }

  public int getDefaultCycleLength() {
    return defaultCycleLength;
  }

  public int getReminderDaysBefore() {
    return reminderDaysBefore;
  }

  public LocalTime getNotificationTime() {
    return notificationTime;
  }
}
