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
        // Expect success count 1
        // If logic is broken, it will likely return "Import Complete: 0 Updated"
        // because it searched for "'123456"
        Assertions.assertTrue(result.contains("1 Updated"), "Should update 1 record. Result: " + result);
        Assertions.assertEquals("UTR001", mockEntry.getUtrNumber(), "UTR should be set (stripping quote if present)");
    }
}
