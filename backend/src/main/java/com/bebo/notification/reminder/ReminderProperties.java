package com.bebo.notification.reminder;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bebo.reminder")
public class ReminderProperties {

  private boolean enabled = true;

  private String cron = "0 * * * * *";

  private String retryCron = "15 * * * * *";

  private int retryBatchSize = 100;

  private Duration firstRetryDelay = Duration.ofMinutes(5);

  private Duration secondRetryDelay = Duration.ofMinutes(15);

  private Duration thirdRetryDelay = Duration.ofMinutes(30);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public String getRetryCron() {
    return retryCron;
  }

  public void setRetryCron(String retryCron) {
    this.retryCron = retryCron;
  }

  public int getRetryBatchSize() {
    return retryBatchSize;
  }

  public void setRetryBatchSize(int retryBatchSize) {
    this.retryBatchSize = retryBatchSize;
  }

  public Duration getFirstRetryDelay() {
    return firstRetryDelay;
  }

  public void setFirstRetryDelay(Duration firstRetryDelay) {
    this.firstRetryDelay = firstRetryDelay;
  }

  public Duration getSecondRetryDelay() {
    return secondRetryDelay;
  }

  public void setSecondRetryDelay(Duration secondRetryDelay) {
    this.secondRetryDelay = secondRetryDelay;
  }

  public Duration getThirdRetryDelay() {
    return thirdRetryDelay;
  }

  public void setThirdRetryDelay(Duration thirdRetryDelay) {
    this.thirdRetryDelay = thirdRetryDelay;
  }
}
