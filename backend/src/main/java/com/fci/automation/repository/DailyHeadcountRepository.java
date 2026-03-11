package com.fci.automation.repository;

import com.fci.automation.entity.DailyHeadcount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyHeadcountRepository extends JpaRepository<DailyHeadcount, Long> {
    Optional<DailyHeadcount> findByDate(LocalDate date);
}
