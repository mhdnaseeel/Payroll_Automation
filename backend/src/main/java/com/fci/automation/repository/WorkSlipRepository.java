package com.fci.automation.repository;

import com.fci.automation.entity.WorkSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.time.LocalDate;

public interface WorkSlipRepository extends JpaRepository<WorkSlip, UUID> {
    boolean existsBySlipNumber(String slipNumber);

    boolean existsBySlipNumberAndCategory(String slipNumber, WorkSlip.WorkSlipCategory category);

    List<WorkSlip> findByCategoryAndEntryDateBetween(WorkSlip.WorkSlipCategory category, LocalDate startDate,
            LocalDate endDate);
}
