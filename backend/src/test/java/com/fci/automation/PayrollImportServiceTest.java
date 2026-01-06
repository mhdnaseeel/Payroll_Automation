package com.fci.automation;

import com.fci.automation.entity.Employee;
import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.entity.PayrollPeriod;
import com.fci.automation.repository.EmployeeRepository;
import com.fci.automation.repository.PayrollEntryRepository;
import com.fci.automation.repository.PayrollPeriodRepository;
import com.fci.automation.service.PayrollImportService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

@SpringBootTest
public class PayrollImportServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PayrollEntryRepository entryRepository;

    @Mock
    private PayrollPeriodRepository periodRepository;

    @InjectMocks
    private PayrollImportService importService;

    @Test
    public void testImportUtrData_WithLeadingSingleQuotes() {
        UUID periodId = UUID.randomUUID();
        PayrollPeriod mockPeriod = new PayrollPeriod();
        mockPeriod.setId(periodId);

        Mockito.when(periodRepository.findById(periodId)).thenReturn(Optional.of(mockPeriod));

        // Sample TSV content with leading quotes, similar to the failing file
        // Header: Account Number (tab) UTR Number
        // Row: '123456 (tab) 'UTR001
        String tsvContent = "Account Number\tUTR Number\n'123456\t'UTR001";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xls",
                "application/vnd.ms-excel",
                tsvContent.getBytes());

        // Mock finding employee by account number "123456" (without quote)
        Employee mockEmployee = new Employee();
        mockEmployee.setId(UUID.randomUUID());
        Mockito.when(employeeRepository.findByBankAccountNo("123456")).thenReturn(Optional.of(mockEmployee));

        // Mock finding existing entry
        PayrollEntry mockEntry = new PayrollEntry();
        Mockito.when(entryRepository.findByPeriodIdAndEmployeeId(periodId, mockEmployee.getId()))
                .thenReturn(Optional.of(mockEntry));

        // Act
        String result = importService.importUtrData(file, periodId);

        // Assert
        Assertions.assertTrue(result.contains("1 Updated"), "Should update 1 record. Result: " + result);
        Assertions.assertEquals("UTR001", mockEntry.getUtrNumber(), "UTR should be set (stripping quote if present)");
    }

    @Test
    public void testImportPayroll_WithTextAndEmptyValues() throws Exception {
        UUID periodId = UUID.randomUUID();
        PayrollPeriod mockPeriod = new PayrollPeriod();
        mockPeriod.setId(periodId);

        Mockito.when(periodRepository.findById(periodId)).thenReturn(Optional.of(mockPeriod));

        // Employee Mock
        Employee mockEmployee = new Employee();
        mockEmployee.setId(UUID.randomUUID());
        mockEmployee.setCategory(Employee.Category.HL); // Regular
        Mockito.when(employeeRepository.findByMemberId("MEM01")).thenReturn(Optional.of(mockEmployee));

        // Entry Mock
        PayrollEntry mockEntry = new PayrollEntry();
        mockEntry.setId(UUID.randomUUID());
        Mockito.when(entryRepository.findByPeriodIdAndEmployeeId(periodId, mockEmployee.getId()))
                .thenReturn(Optional.of(mockEntry));

        // Create Excel with 2 rows
        // MemberID | Name | Days | Wages | Advance
        // MEM01 | Test | 20 | 1000 | '1500 (Text)
        // MEM01 | Test | 20 | 1000 | (Empty) -> Should reset to 0

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Import");

            // Header
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("MEMBER_ID");
            header.createCell(2).setCellValue("DAYS_WORKED");
            header.createCell(3).setCellValue("WAGES_EARNED");
            header.createCell(4).setCellValue("ADVANCE");

            // Row 1: Text Advance "1500"
            org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("MEM01");
            row1.createCell(2).setCellValue(20);
            row1.createCell(3).setCellValue(1000);
            row1.createCell(4).setCellValue("1500"); // Text format

            workbook.write(out);

            MockMultipartFile file1 = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

            // Act 1
            importService.importPayroll(file1, periodId);

            // Assert 1
            Assertions.assertEquals(new java.math.BigDecimal("1500"), mockEntry.getAdvanceDeduction(),
                    "Should parse text '1500' as BigDecimal");

            // Row 2: Empty Advance
            out.reset();
            try (org.apache.poi.ss.usermodel.Workbook wb2 = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                org.apache.poi.ss.usermodel.Sheet s2 = wb2.createSheet("Import");
                s2.createRow(0); // Header (empty is fine for this test logic as strict check skips it, but lets
                                 // keep index)
                org.apache.poi.ss.usermodel.Row r1 = s2.createRow(1);
                r1.createCell(0).setCellValue("MEM01");
                r1.createCell(2).setCellValue(20);
                r1.createCell(3).setCellValue(1000);
                r1.createCell(4); // Null/Blank (Advance)

                wb2.write(out);
            }

            MockMultipartFile file2 = new MockMultipartFile("file", "test2.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

            // Act 2
            importService.importPayroll(file2, periodId);

            // Assert 2
            Assertions.assertEquals(java.math.BigDecimal.ZERO, mockEntry.getAdvanceDeduction(),
                    "Should reset Advance to 0 if cell is empty");
        }
    }
}
