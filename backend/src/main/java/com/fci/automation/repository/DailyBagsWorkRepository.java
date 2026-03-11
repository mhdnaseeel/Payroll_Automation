package com.fci.automation.repository;

import com.fci.automation.entity.DailyBagsWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyBagsWorkRepository extends JpaRepository<DailyBagsWork, UUID> {
    Optional<DailyBagsWork> findByDate(LocalDate date);
}
