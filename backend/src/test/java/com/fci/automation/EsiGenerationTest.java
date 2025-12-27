package com.fci.automation;

import com.fci.automation.entity.Employee;
import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.entity.PayrollPeriod;
import com.fci.automation.repository.PayrollEntryRepository;
import com.fci.automation.repository.PayrollPeriodRepository;
import com.fci.automation.service.ReportService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EsiGenerationTest {

    @Mock
    private PayrollEntryRepository entryRepository;

    @Mock
    private PayrollPeriodRepository periodRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void testGenerateEsiExcel_LogicVerification() throws Exception {
        // Ensure Template Exists (Pre-check)
        File template = new File("ESI.xls");
        if (!template.exists()) {
            Assertions.fail("ESI.xls template not found in project root!");
        }

        UUID periodId = UUID.randomUUID();
        PayrollPeriod period = new PayrollPeriod();
        period.setLastWorkingDay(LocalDate.of(2025, 11, 30));
        when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));

        // 1. Casual Labour Entry (Days=10, Input=9999 [Incorrect], Logic should use
        // 10*541=5410)
        PayrollEntry clEntry = new PayrollEntry();
        Employee clEmp = new Employee();
        clEmp.setCategory(Employee.Category.CL);
        clEmp.setIpNumber("1111111111");
        clEmp.setFullName("Casual Worker");
        clEntry.setEmployee(clEmp);
        clEntry.setDaysWorked(10);
        clEntry.setWagesEarned(new BigDecimal("9999")); // Should be ignored

        // 2. Regular Employee (Input=5000)
        PayrollEntry hlEntry = new PayrollEntry();
        Employee hlEmp = new Employee();
        hlEmp.setCategory(Employee.Category.HL);
        hlEmp.setIpNumber("2222222222");
        hlEmp.setFullName("Regular Worker");
        hlEntry.setEmployee(hlEmp);
        hlEntry.setDaysWorked(20);
        hlEntry.setWagesEarned(new BigDecimal("5000")); // Should be used

        // 3. Zero Wage Employee (Reason Code Check)
        PayrollEntry zeroEntry = new PayrollEntry();
        Employee zeroEmp = new Employee();
        zeroEmp.setCategory(Employee.Category.HL);
        zeroEmp.setIpNumber("3333333333");
        zeroEmp.setFullName("Zero Worker");
        zeroEntry.setEmployee(zeroEmp);
        zeroEntry.setDaysWorked(0);
        zeroEntry.setWagesEarned(BigDecimal.ZERO);

        when(entryRepository.findByPeriodId(periodId)).thenReturn(Arrays.asList(clEntry, hlEntry, zeroEntry));

        // Execute
        byte[] result = reportService.generateEsiExcel(periodId);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);

        // Verify Content
        try (Workbook workbook = new HSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);

            // Row 1: Casual Labour
            Row r1 = sheet.getRow(1);
            Assertions.assertEquals("Casual Worker", r1.getCell(1).getStringCellValue());
            Assertions.assertEquals(10.0, r1.getCell(2).getNumericCellValue());
            Assertions.assertEquals(5410.0, r1.getCell(3).getNumericCellValue(), "CL Wages should be Days*541");
            Assertions.assertEquals("0", r1.getCell(4).getStringCellValue(), "Reason Code should be 0");
            Assertions.assertEquals("30/11/2025", r1.getCell(5).getStringCellValue());

            // Row 2: Regular Worker
            Row r2 = sheet.getRow(2);
            Assertions.assertEquals("Regular Worker", r2.getCell(1).getStringCellValue());
            Assertions.assertEquals(5000.0, r2.getCell(3).getNumericCellValue(), "HL Wages should be Input");
            Assertions.assertEquals("0", r2.getCell(4).getStringCellValue());

            // Row 3: Zero Worker
            Row r3 = sheet.getRow(3);
            Assertions.assertEquals(0.0, r3.getCell(3).getNumericCellValue());
            Assertions.assertEquals("1", r3.getCell(4).getStringCellValue(), "Reason Code should be 1 for 0 wages");
        }
    }
}
