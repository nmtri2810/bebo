package com.bebo.cycle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  @Query(
      """
      select
        cycleRecord.id as id,
        cycleRecord.startDate as startDate
      from CycleRecord cycleRecord
      where cycleRecord.user.id = :userId
      order by cycleRecord.startDate desc
      """)
  List<CycleStartProjection> findRecentCycleStarts(@Param("userId") UUID userId, Pageable pageable);

  interface CycleStartProjection {

    UUID getId();

    LocalDate getStartDate();
  }
}
