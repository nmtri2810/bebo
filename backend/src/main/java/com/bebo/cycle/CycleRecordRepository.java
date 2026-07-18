package com.bebo.cycle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CycleRecordRepository extends JpaRepository<CycleRecord, UUID> {

  List<CycleRecord> findAllByUserIdOrderByStartDateDesc(UUID userId);

  List<CycleRecord> findTop7ByUserIdOrderByStartDateDesc(UUID userId);

  Optional<CycleRecord> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByUserIdAndStartDate(UUID userId, LocalDate startDate);

  boolean existsByUserIdAndStartDateAndIdNot(UUID userId, LocalDate startDate, UUID excludedId);
}
