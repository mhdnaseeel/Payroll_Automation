package com.fci.automation;

import com.fci.automation.entity.Employee;
import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.entity.PayrollPeriod;
import com.fci.automation.repository.PayrollEntryRepository;
import com.fci.automation.repository.PayrollPeriodRepository;
import com.fci.automation.service.ReportService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ReportServiceTest {

    @Mock
    private PayrollPeriodRepository periodRepository;

    @Mock
    private PayrollEntryRepository entryRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void testGenerateAttendanceRegisterPdf() {
        // 1. Mock Data
        UUID periodId = UUID.randomUUID();
        PayrollPeriod period = new PayrollPeriod();
        period.setId(periodId);
        period.setMonth(9); // September
        period.setYear(2025);

        Employee emp = new Employee();
        emp.setFullName("Test User");
        emp.setMemberId("101");
        emp.setCategory(Employee.Category.CL);

        PayrollEntry entry = new PayrollEntry();
        entry.setEmployee(emp);
        entry.setDaysWorked(15);
        entry.setWagesEarned(new BigDecimal("15000"));
        Set<Integer> activeDays = new HashSet<>();
        activeDays.add(1);
        activeDays.add(15);
        activeDays.add(30);
        entry.setActiveDays(activeDays);

        // 2. Mock Behavior
        when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(entryRepository.findByPeriodId(periodId)).thenReturn(Collections.singletonList(entry));

        // 3. Execute
        byte[] pdf = reportService.generateAttendanceRegisterPdf(periodId);

        // 4. Verify
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        System.out.println("PDF Generated Successfully. Size: " + pdf.length + " bytes.");
    }
}
