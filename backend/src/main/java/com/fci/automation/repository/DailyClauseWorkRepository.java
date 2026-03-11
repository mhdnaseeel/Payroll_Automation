package com.fci.automation.repository;

import com.fci.automation.entity.DailyClauseWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyClauseWorkRepository extends JpaRepository<DailyClauseWork, UUID> {
    Optional<DailyClauseWork> findByDate(LocalDate date);
}
