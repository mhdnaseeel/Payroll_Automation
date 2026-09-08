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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @Test
    public void testBankSummaryAndBulkTxtGeneration() {
        UUID periodId = UUID.randomUUID();
        PayrollPeriod period = new PayrollPeriod();
        period.setId(periodId);
        period.setMonth(11);
        period.setYear(2025);

        Employee sbiEmp = new Employee();
        sbiEmp.setMemberId("101");
        sbiEmp.setFullName("SBI User");
        sbiEmp.setBankAccountNo("1234567890");
        sbiEmp.setIfscCode("SBIN0001234");

        PayrollEntry sbiEntry = new PayrollEntry();
        sbiEntry.setEmployee(sbiEmp);
        sbiEntry.setNetPayable(new BigDecimal("25000.40"));

        Employee otherEmp = new Employee();
        otherEmp.setMemberId("102");
        otherEmp.setFullName("Other Bank User");
        otherEmp.setBankAccountNo("9876543210");
        otherEmp.setIfscCode("HDFC0005678");

        PayrollEntry otherEntry = new PayrollEntry();
        otherEntry.setEmployee(otherEmp);
        otherEntry.setNetPayable(new BigDecimal("18000.70"));

        when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(entryRepository.findByPeriodId(periodId)).thenReturn(Arrays.asList(sbiEntry, otherEntry));

        // 1. Verify Summary
        Map<String, Object> summary = reportService.getBankSummary(periodId);
        org.junit.jupiter.api.Assertions.assertEquals(1, summary.get("sameBankCount"));
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("25000"), summary.get("sameBankAmount"));
        org.junit.jupiter.api.Assertions.assertEquals(1, summary.get("otherBankCount"));
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("18001"), summary.get("otherBankAmount"));
        org.junit.jupiter.api.Assertions.assertEquals(2, summary.get("totalCount"));
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("43001"), summary.get("totalAmount"));

        // 2. Verify SAME_BANK bulk text
        String sameBankTxt = reportService.generateBulkTxt(periodId, java.time.LocalDate.of(2025, 11, 15), "SAME_BANK");
        String[] sameLines = sameBankTxt.trim().split("\n");
        org.junit.jupiter.api.Assertions.assertEquals(2, sameLines.length);
        org.junit.jupiter.api.Assertions.assertEquals("44145351821#17242#15/11/2025#25000###NASAR PK#ST", sameLines[0]);
        org.junit.jupiter.api.Assertions.assertEquals("1234567890#SBIN0001234#15/11/2025##25000##NASAR PK#ST", sameLines[1]);

        // 3. Verify OTHER_BANK bulk text
        String otherBankTxt = reportService.generateBulkTxt(periodId, java.time.LocalDate.of(2025, 11, 15), "OTHER_BANK");
        String[] otherLines = otherBankTxt.trim().split("\n");
        org.junit.jupiter.api.Assertions.assertEquals(2, otherLines.length);
        org.junit.jupiter.api.Assertions.assertEquals("44145351821#17242#15/11/2025#18001###NASAR PK#NEFT", otherLines[0]);
        org.junit.jupiter.api.Assertions.assertEquals("9876543210#HDFC0005678#15/11/2025##18001##NASAR PK#NEFT", otherLines[1]);
    }
}
