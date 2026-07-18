package com.bebo.settings;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CycleSettingsRepository extends JpaRepository<CycleSettings, UUID> {

  Optional<CycleSettings> findByUserId(UUID userId);
}
