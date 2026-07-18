package com.bebo.cycle;

import com.bebo.common.model.BaseEntity;
import com.bebo.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "cycle_records")
public class CycleRecord extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  protected CycleRecord() {}

  private CycleRecord(User user, LocalDate startDate) {
    this.user = user;
    this.startDate = startDate;
  }

  public static CycleRecord create(User user, LocalDate startDate) {
    return new CycleRecord(user, startDate);
  }

  public void updateStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public User getUser() {
    return user;
  }

  public LocalDate getStartDate() {
    return startDate;
  }
}
