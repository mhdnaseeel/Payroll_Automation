package com.fci.automation.repository;

import com.fci.automation.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByMemberId(String memberId);

    Optional<Employee> findByUanNumber(String uanNumber);

    Optional<Employee> findByIpNumber(String ipNumber);

    Optional<Employee> findByBankAccountNo(String bankAccountNo);
}
