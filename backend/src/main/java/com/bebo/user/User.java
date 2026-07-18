package com.bebo.user;

import com.bebo.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.ZoneId;

@Entity
@Table(name = "app_users")
public class User extends BaseEntity {

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(nullable = false, length = 100)
  private String timezone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status;

  protected User() {}

  private User(String email, String passwordHash, String timezone) {
    this.email = normalizeEmail(email);
    this.passwordHash = passwordHash;
    this.timezone = validateTimezone(timezone);
    this.status = UserStatus.ACTIVE;
  }

  public static User create(String email, String passwordHash, String timezone) {
    return new User(email, passwordHash, timezone);
  }

  public void updateTimezone(String timezone) {
    this.timezone = validateTimezone(timezone);
  }

  public void disable() {
    this.status = UserStatus.DISABLED;
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private static String validateTimezone(String timezone) {
    ZoneId.of(timezone);
    return timezone;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getTimezone() {
    return timezone;
  }

  public UserStatus getStatus() {
    return status;
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }
}
