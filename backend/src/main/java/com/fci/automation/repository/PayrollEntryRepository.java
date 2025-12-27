package com.fci.automation.repository;

import com.fci.automation.entity.PayrollEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface PayrollEntryRepository extends JpaRepository<PayrollEntry, UUID> {
    List<PayrollEntry> findByPeriodId(UUID periodId);

    java.util.Optional<PayrollEntry> findByPeriodIdAndEmployeeId(UUID periodId, UUID employeeId);
}
