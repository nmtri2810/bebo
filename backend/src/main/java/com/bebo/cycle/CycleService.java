package com.bebo.cycle;

import com.bebo.common.exception.BadRequestException;
import com.bebo.common.exception.ConflictException;
import com.bebo.common.exception.NotFoundException;
import com.bebo.cycle.dto.CreateCycleRecordRequest;
import com.bebo.cycle.dto.CyclePredictionResponse;
import com.bebo.cycle.dto.CycleRecordResponse;
import com.bebo.cycle.dto.UpdateCycleRecordRequest;
import com.bebo.settings.CycleSettings;
import com.bebo.settings.CycleSettingsRepository;
import com.bebo.user.User;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleService {

  private final CycleRecordRepository cycleRecordRepository;

  private final CycleSettingsRepository cycleSettingsRepository;

  public CycleService(
      CycleRecordRepository cycleRecordRepository,
      CycleSettingsRepository cycleSettingsRepository) {
    this.cycleRecordRepository = cycleRecordRepository;

    this.cycleSettingsRepository = cycleSettingsRepository;
  }

  @Transactional
  public CycleRecordResponse create(User user, CreateCycleRecordRequest request) {
    LocalDate startDate = request.startDate();

    validateStartDate(user, startDate);

    boolean alreadyExists =
        cycleRecordRepository.existsByUser_IdAndStartDate(user.getId(), startDate);

    if (alreadyExists) {
      throw new ConflictException("A cycle record already exists for this date");
    }

    CycleRecord cycleRecord = CycleRecord.create(user, startDate);

    CycleRecord savedRecord = cycleRecordRepository.save(cycleRecord);

    return toResponse(savedRecord, null);
  }

  @Transactional(readOnly = true)
  public List<CycleRecordResponse> getHistory(User user) {
    List<CycleRecord> records =
        cycleRecordRepository.findAllByUser_IdOrderByStartDateDesc(user.getId());

    List<CycleRecordResponse> response = new ArrayList<>(records.size());

    for (int index = 0; index < records.size(); index++) {
      CycleRecord currentRecord = records.get(index);

      Long cycleLengthFromPrevious = null;

      /*
       * Danh sách đang giảm dần:
       *
       * index 0 = record mới nhất
       * index 1 = record trước đó
       */
      if (index + 1 < records.size()) {
        CycleRecord previousRecord = records.get(index + 1);

        cycleLengthFromPrevious =
            ChronoUnit.DAYS.between(previousRecord.getStartDate(), currentRecord.getStartDate());
      }

      response.add(toResponse(currentRecord, cycleLengthFromPrevious));
    }

    return response;
  }

  @Transactional
  public CycleRecordResponse update(User user, UUID recordId, UpdateCycleRecordRequest request) {
    LocalDate newStartDate = request.startDate();

    validateStartDate(user, newStartDate);

    CycleRecord record = requireRecord(user, recordId);

    boolean duplicateExists =
        cycleRecordRepository.existsByUser_IdAndStartDateAndIdNot(
            user.getId(), newStartDate, recordId);

    if (duplicateExists) {
      throw new ConflictException("A cycle record already exists for this date");
    }

    record.updateStartDate(newStartDate);

    /*
     * Không bắt buộc gọi save().
     * Entity đang được quản lý trong transaction,
     * Hibernate sẽ thực hiện dirty checking.
     */
    return toResponse(record, null);
  }

  @Transactional
  public void delete(User user, UUID recordId) {
    CycleRecord record = requireRecord(user, recordId);

    cycleRecordRepository.delete(record);
  }

  @Transactional(readOnly = true)
  public CyclePredictionResponse getPrediction(User user) {
    List<CycleRecord> records =
        cycleRecordRepository.findTop7ByUser_IdOrderByStartDateDesc(user.getId());

    if (records.isEmpty()) {
      throw new NotFoundException("Add at least one cycle record before requesting a prediction");
    }

    CycleSettings settings =
        cycleSettingsRepository
            .findByUser_Id(user.getId())
            .orElseThrow(() -> new NotFoundException("Cycle settings were not found"));

    List<LocalDate> startDatesDescending = records.stream().map(CycleRecord::getStartDate).toList();

    CyclePredictionCalculator.Calculation calculation =
        CyclePredictionCalculator.calculateAverage(
            startDatesDescending, settings.getDefaultCycleLength());

    LocalDate lastPeriodStartDate = records.getFirst().getStartDate();

    LocalDate expectedNextPeriodDate =
        lastPeriodStartDate.plusDays(calculation.averageCycleLength());

    LocalDate reminderDate = expectedNextPeriodDate.minusDays(settings.getReminderDaysBefore());

    LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

    long daysRemaining = ChronoUnit.DAYS.between(today, expectedNextPeriodDate);

    return new CyclePredictionResponse(
        lastPeriodStartDate,
        calculation.averageCycleLength(),
        expectedNextPeriodDate,
        reminderDate,
        daysRemaining,
        calculation.source(),
        calculation.historicalCyclesUsed());
  }

  private CycleRecord requireRecord(User user, UUID recordId) {
    return cycleRecordRepository
        .findByIdAndUser_Id(recordId, user.getId())
        .orElseThrow(() -> new NotFoundException("Cycle record was not found"));
  }

  private void validateStartDate(User user, LocalDate startDate) {
    LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

    if (startDate.isAfter(today)) {
      throw new BadRequestException("Cycle start date cannot be in the future");
    }
  }

  private CycleRecordResponse toResponse(CycleRecord record, Long cycleLengthFromPrevious) {
    return new CycleRecordResponse(
        record.getId(),
        record.getStartDate(),
        cycleLengthFromPrevious,
        record.getCreatedAt(),
        record.getUpdatedAt());
  }
}
