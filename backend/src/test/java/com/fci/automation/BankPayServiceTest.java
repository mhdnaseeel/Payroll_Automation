package com.fci.automation;

import com.fci.automation.dto.BankPayGenerateRequestDto;
import com.fci.automation.dto.BankPayImportResultDto;
import com.fci.automation.dto.BankPayRecordDto;
import com.fci.automation.entity.Employee;
import com.fci.automation.repository.EmployeeRepository;
import com.fci.automation.service.BankPayService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class BankPayServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private BankPayService bankPayService;

    private Employee john;
    private Employee jane;

    @BeforeEach
    public void setup() {
        john = new Employee();
        john.setId(UUID.randomUUID());
        john.setMemberId("EMP001");
        john.setFullName("John Doe");
        john.setBankAccountNo("1234567890");
        john.setIfscCode("SBIN0001234"); // SBI Same Bank

        jane = new Employee();
        jane.setId(UUID.randomUUID());
        jane.setMemberId("EMP002");
        jane.setFullName("Jane Smith");
        jane.setBankAccountNo("9876543210");
        jane.setIfscCode("HDFC0005678"); // Other Bank

        Mockito.when(employeeRepository.findAll()).thenReturn(Arrays.asList(john, jane));
    }

    @Test
    public void testParseAndMatchExcel_ClassificationAndSummary() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();

        // Header
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Employee Name");
        header.createCell(1).setCellValue("Amount");

        // Row 1: John Doe (SBI) - check decimal round down (3 < 5)
        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("John Doe");
        r1.createCell(1).setCellValue(25000.34); // Should round to 25000

        // Row 2: Jane Smith (HDFC) - check decimal round up (7 >= 5)
        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("Jane Smith");
        r2.createCell(1).setCellValue(18000.74); // Should round to 18001

        // Row 3: Not Found
        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Unknown Person");
        r3.createCell(1).setCellValue(5000);

        // Row 4: Zero amount check
        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Zero Person");
        r4.createCell(1).setCellValue(0); // Should be INVALID

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        BankPayImportResultDto result = bankPayService.parseAndMatchExcel(in);

        Assertions.assertEquals(4, result.getTotalRecords());
        Assertions.assertEquals(2, result.getTotalSuccess());
        Assertions.assertEquals(2, result.getTotalFailed());

        // Same Bank Checks (rounded to 25000)
        Assertions.assertEquals(1, result.getSameBankCount());
        Assertions.assertEquals(new BigDecimal("25000"), result.getSameBankAmount());

        // Other Bank Checks (rounded to 18001)
        Assertions.assertEquals(1, result.getOtherBankCount());
        Assertions.assertEquals(new BigDecimal("18001"), result.getOtherBankAmount());

        // Total Amount
        Assertions.assertEquals(new BigDecimal("43001"), result.getTotalAmount());

        List<BankPayRecordDto> records = result.getRecords();
        Assertions.assertEquals("SAME_BANK", records.get(0).getBankCategory());
        Assertions.assertEquals(new BigDecimal("25000"), records.get(0).getAmount());
        Assertions.assertEquals("OTHER_BANK", records.get(1).getBankCategory());
        Assertions.assertEquals(new BigDecimal("18001"), records.get(1).getAmount());
        Assertions.assertEquals("NOT_FOUND", records.get(2).getStatus());
        
        // Zero person check
        Assertions.assertEquals("INVALID", records.get(3).getStatus());
        Assertions.assertEquals("Amount must be greater than zero", records.get(3).getReason());
    }

    @Test
    public void testGenerateBankFile_SameBankVsOtherBank() {
        BankPayRecordDto r1 = new BankPayRecordDto("EMP001", "John Doe", "1234567890", "SBIN0001234", new BigDecimal("25000"), "READY", "Ready", "SAME_BANK");
        BankPayRecordDto r2 = new BankPayRecordDto("EMP002", "Jane Smith", "9876543210", "HDFC0005678", new BigDecimal("18000"), "READY", "Ready", "OTHER_BANK");

        // Same Bank Request
        BankPayGenerateRequestDto sameBankReq = new BankPayGenerateRequestDto(
                Arrays.asList(r1, r2), "2026-08-05", "44145351821", "17242", "COMPANY NAME", "SAME_BANK"
        );
        String sameBankTxt = bankPayService.generateBankFile(sameBankReq);
        String[] sameLines = sameBankTxt.split("\n");
        Assertions.assertEquals(2, sameLines.length);
        Assertions.assertEquals("44145351821#17242#05/08/2026#25000###COMPANY NAME#ST", sameLines[0]);
        Assertions.assertEquals("1234567890#SBIN0001234#05/08/2026##25000##COMPANY NAME#ST", sameLines[1]);

        // Other Bank Request
        BankPayGenerateRequestDto otherBankReq = new BankPayGenerateRequestDto(
                Arrays.asList(r1, r2), "2026-08-05", "44145351821", "17242", "COMPANY NAME", "OTHER_BANK"
        );
        String otherBankTxt = bankPayService.generateBankFile(otherBankReq);
        String[] otherLines = otherBankTxt.split("\n");
        Assertions.assertEquals(2, otherLines.length);
        Assertions.assertEquals("44145351821#17242#05/08/2026#18000###COMPANY NAME#NEFT", otherLines[0]);
        Assertions.assertEquals("9876543210#HDFC0005678#05/08/2026##18000##COMPANY NAME#NEFT", otherLines[1]);
    }
}
