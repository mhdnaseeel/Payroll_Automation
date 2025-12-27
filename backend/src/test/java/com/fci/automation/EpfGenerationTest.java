package com.fci.automation;

import com.fci.automation.entity.Employee;
import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.repository.PayrollEntryRepository;
import com.fci.automation.service.ReportService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EpfGenerationTest {

    @Mock
    private PayrollEntryRepository entryRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void testGenerateEpfTxt_LogicVerification() {
        UUID periodId = UUID.randomUUID();

        // 1. Casual Labour (Days 10, Gross = 5410)
        // EPF (12%) = 649
        // EPS (8.33%) = 451
        // Diff = 649 - 451 = 198
        // NCP = 20
        PayrollEntry clEntry = createEntry(Employee.Category.CL, 10, new BigDecimal("0"));
        clEntry.getEmployee().setUanNumber("CL_UAN");

        // 2. High Earner (Wages 20000)
        // Gross = 20000
        // EPF Wages = 20000
        // EPS Wages = 15000 (Cap)
        // EPF Contri (12% of 20000) = 2400
        // EPS Contri (8.33% of 20000 -> 1666, Cap 1250) = 1250
        // Diff (Logic: IF EPS=1250 THEN 550) = 550
        // NCP = 30 - 20 = 10
        PayrollEntry highEntry = createEntry(Employee.Category.HL, 20, new BigDecimal("20000"));
        highEntry.getEmployee().setUanNumber("HIGH_UAN");

        when(entryRepository.findByPeriodId(periodId)).thenReturn(Arrays.asList(clEntry, highEntry));

        // Execute
        String txt = reportService.generateEpfTxt(periodId);
        String[] lines = txt.split("\n");

        Assertions.assertEquals(2, lines.length);

        // Verify CL Line
        // CL_UAN#~#Name#~#5410#~#5410#~#5410#~#5410#~#649#~#451#~#198#~#20#~#0
        String[] clParts = lines[0].split("#~#");
        Assertions.assertEquals("CL_UAN", clParts[0]);
        Assertions.assertEquals("5410", clParts[2], "Gross");
        Assertions.assertEquals("5410", clParts[3], "EPF Wages");
        Assertions.assertEquals("649", clParts[6], "EPF Contri");
        Assertions.assertEquals("451", clParts[7], "EPS Contri");
        Assertions.assertEquals("198", clParts[8], "Diff");
        Assertions.assertEquals("20", clParts[9], "NCP");

        // Verify High Earner Line
        // HIGH_UAN#~#Name#~#20000#~#20000#~#15000#~#15000#~#2400#~#1250#~#550#~#10#~#0
        String[] highParts = lines[1].split("#~#");
        Assertions.assertEquals("HIGH_UAN", highParts[0]);
        Assertions.assertEquals("20000", highParts[2], "Gross");
        Assertions.assertEquals("15000", highParts[4], "EPS Wages (Capped)");
        Assertions.assertEquals("2400", highParts[6], "EPF Contri (Uncapped Base)");
        Assertions.assertEquals("1250", highParts[7], "EPS Contri (Capped)");
        Assertions.assertEquals("550", highParts[8], "Diff (Special Logic)");
    }

    private PayrollEntry createEntry(Employee.Category cat, int days, BigDecimal wages) {
        PayrollEntry entry = new PayrollEntry();
        Employee emp = new Employee();
        emp.setCategory(cat);
        emp.setFullName("Name");
        entry.setEmployee(emp);
        entry.setDaysWorked(days);
        entry.setWagesEarned(wages);
        return entry;
    }
}
