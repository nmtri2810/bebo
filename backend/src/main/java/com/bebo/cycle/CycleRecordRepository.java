package com.bebo.cycle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CycleRecordRepository extends JpaRepository<CycleRecord, UUID> {

  List<CycleRecord> findAllByUser_IdOrderByStartDateDesc(UUID userId);

  /*
   * 7 ngày bắt đầu tạo ra tối đa 6 khoảng
   * thời gian giữa các chu kỳ.
   */
  List<CycleRecord> findTop7ByUser_IdOrderByStartDateDesc(UUID userId);

  Optional<CycleRecord> findByIdAndUser_Id(UUID recordId, UUID userId);

  boolean existsByUser_IdAndStartDate(UUID userId, LocalDate startDate);

  boolean existsByUser_IdAndStartDateAndIdNot(
      UUID userId, LocalDate startDate, UUID excludedRecordId);
}
