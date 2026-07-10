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
                // 10*556=5560)
                PayrollEntry clEntry = new PayrollEntry();
                Employee clEmp = new Employee();
                clEmp.setCategory(Employee.Category.CL);
                clEmp.setIpNumber("1111111111");
                clEmp.setFullName("Casual Worker");
                clEmp.setStatus(Employee.Status.ACTIVE);
                clEntry.setEmployee(clEmp);
                clEntry.setDaysWorked(10);
                clEntry.setWagesEarned(new BigDecimal("9999")); // Should be ignored

                // 2. Regular Employee (Input=5000)
                PayrollEntry hlEntry = new PayrollEntry();
                Employee hlEmp = new Employee();
                hlEmp.setCategory(Employee.Category.HL);
                hlEmp.setIpNumber("2222222222");
                hlEmp.setFullName("Regular Worker");
                hlEmp.setStatus(Employee.Status.ACTIVE);
                hlEntry.setEmployee(hlEmp);
                hlEntry.setDaysWorked(20);
                hlEntry.setWagesEarned(new BigDecimal("5000")); // Should be used

                // 3. Zero Wage Employee - ACTIVE (On Leave, Reason Code 1)
                PayrollEntry zeroEntry = new PayrollEntry();
                Employee zeroEmp = new Employee();
                zeroEmp.setCategory(Employee.Category.HL);
                zeroEmp.setIpNumber("3333333333");
                zeroEmp.setFullName("Zero Worker");
                zeroEmp.setStatus(Employee.Status.ACTIVE);
                zeroEntry.setEmployee(zeroEmp);
                zeroEntry.setDaysWorked(0);
                zeroEntry.setWagesEarned(BigDecimal.ZERO);

                // 4. INACTIVE Employee - Left Service (Reason Code 2, with Last Working Day)
                PayrollEntry inactiveEntry = new PayrollEntry();
                Employee inactiveEmp = new Employee();
                inactiveEmp.setCategory(Employee.Category.HL);
                inactiveEmp.setIpNumber("4444444444");
                inactiveEmp.setFullName("Left Worker");
                inactiveEmp.setStatus(Employee.Status.INACTIVE);
                inactiveEmp.setInactiveDate(LocalDate.of(2025, 11, 15));
                inactiveEntry.setEmployee(inactiveEmp);
                inactiveEntry.setDaysWorked(0);
                inactiveEntry.setWagesEarned(BigDecimal.ZERO);

                when(entryRepository.findByPeriodId(periodId))
                                .thenReturn(Arrays.asList(clEntry, hlEntry, zeroEntry, inactiveEntry));

                // Execute
                byte[] result = reportService.generateEsiExcel(periodId);
                Assertions.assertNotNull(result);
                Assertions.assertTrue(result.length > 0);

                // Verify Content
                try (Workbook workbook = new HSSFWorkbook(new ByteArrayInputStream(result))) {
                        Sheet sheet = workbook.getSheetAt(0);

                        // Row 1: Casual Labour (days > 0 → Reason Code "0", Last Working Day blank)
                        Row r1 = sheet.getRow(1);
                        Assertions.assertEquals("Casual Worker", r1.getCell(1).getStringCellValue());
                        Assertions.assertEquals(10.0, r1.getCell(2).getNumericCellValue());
                        Assertions.assertEquals(5560.0, r1.getCell(3).getNumericCellValue(),
                                        "CL Wages should be Days*556");
                        Assertions.assertEquals(0.0, r1.getCell(4).getNumericCellValue(),
                                        "Reason Code should be 0 for days > 0");
                        Assertions.assertNull(r1.getCell(5), "Last Working Day cell should be empty when days > 0");

                        // Row 2: Regular Worker (days > 0 → Reason Code "0", Last Working Day blank)
                        Row r2 = sheet.getRow(2);
                        Assertions.assertEquals("Regular Worker", r2.getCell(1).getStringCellValue());
                        Assertions.assertEquals(5000.0, r2.getCell(3).getNumericCellValue(),
                                        "HL Wages should be Input");
                        Assertions.assertEquals(0.0, r2.getCell(4).getNumericCellValue(),
                                        "Reason Code should be 0 for days > 0");
                        Assertions.assertNull(r2.getCell(5), "Last Working Day cell should be empty when days > 0");

                        // Row 3: Zero Worker ACTIVE (days=0, ACTIVE → Reason Code "1" On Leave, Last
                        // Working Day blank)
                        Row r3 = sheet.getRow(3);
                        Assertions.assertEquals(0.0, r3.getCell(3).getNumericCellValue());
                        Assertions.assertEquals(1.0, r3.getCell(4).getNumericCellValue(),
                                        "Reason Code should be 1 (On Leave) for ACTIVE with 0 days");
                        Assertions.assertNull(r3.getCell(5), "Last Working Day cell should be empty for On Leave");

                        // Row 4: Left Worker INACTIVE (days=0, INACTIVE → Reason Code "2" Left Service,
                        // Last Working Day = inactiveDate)
                        Row r4 = sheet.getRow(4);
                        Assertions.assertEquals("Left Worker", r4.getCell(1).getStringCellValue());
                        Assertions.assertEquals(0.0, r4.getCell(3).getNumericCellValue());
                        Assertions.assertEquals(2.0, r4.getCell(4).getNumericCellValue(),
                                        "Reason Code should be 2 (Left Service) for INACTIVE");
                        Assertions.assertNotNull(r4.getCell(5),
                                        "Last Working Day cell should be created for Left Service");
                        Assertions.assertEquals("15/11/2025", r4.getCell(5).getStringCellValue(),
                                        "Last Working Day should be inactiveDate");
                }
        }
}
