package com.fci.automation.repository;

import com.fci.automation.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, UUID> {
    Optional<PayrollPeriod> findByMonthAndYear(Integer month, Integer year);

    List<PayrollPeriod> findAllByOrderByYearDescMonthDesc();
}
