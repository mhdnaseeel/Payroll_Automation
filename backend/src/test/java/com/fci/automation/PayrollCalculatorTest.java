package com.fci.automation;

import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.service.PayrollCalculatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

public class PayrollCalculatorTest {

    private final PayrollCalculatorService calculator = new PayrollCalculatorService();

    @Test
    public void testCalculation_CasualLabour() {
        // Prepare CL Employee
        com.fci.automation.entity.Employee empOr = new com.fci.automation.entity.Employee();
        empOr.setCategory(com.fci.automation.entity.Employee.Category.CL);

        // Prepare Entry
        PayrollEntry entry = new PayrollEntry();
        entry.setEmployee(empOr);
        entry.setDaysWorked(10);

        calculator.calculate(entry);

        // Verify Wages = 10 * 541 = 5410
        Assertions.assertEquals(new BigDecimal("5410"), entry.getWagesEarned(), "Wages Mismatch for CL");

        // Verify Deductions (Calculated & Stored for Report)
        Assertions.assertEquals(new BigDecimal("649"), entry.getEpfMemberShare(), "EPF Member should be 649");
        Assertions.assertEquals(new BigDecimal("41"), entry.getEsiMemberShare(), "ESI Member should be 41");

        // Verify Contractor Shares
        Assertions.assertEquals(new BigDecimal("649"), entry.getEpfContractorShare(), "EPF Contractor Share Mismatch");
        Assertions.assertEquals(new BigDecimal("176"), entry.getEsiContractorShare(), "ESI Contractor Share Mismatch");

        // Verify Net Pay = Wages (Entry Screen Logic - User Request)
        // Deductions are verified above, but Net Pay here ignores them.
        Assertions.assertEquals(new BigDecimal("5410"), entry.getNetPayable(), "Net Pay Mismatch for CL");
    }

    @Test
    public void testCalculation_EpfCap() {
        // Prepare HL Employee (Regular)
        com.fci.automation.entity.Employee empOr = new com.fci.automation.entity.Employee();
        empOr.setCategory(com.fci.automation.entity.Employee.Category.HL);

        // Prepare Entry
        PayrollEntry entry = new PayrollEntry();
        entry.setEmployee(empOr);
        // Wages > 15000
        entry.setWagesEarned(new BigDecimal("16000"));

        calculator.calculate(entry);

        // EPF Member (No Cap): 16000 * 0.12 = 1920
        Assertions.assertEquals(new BigDecimal("1920"), entry.getEpfMemberShare(),
                "EPF Member Share Mismatch (Should not be capped)");

        // EPF Contractor (Capped): 1800
        Assertions.assertEquals(new BigDecimal("1800"), entry.getEpfContractorShare(),
                "EPF Contractor Share Mismatch (Should be capped)");

        // ESI Contractor: 16000 * 0.0325 = 520
        Assertions.assertEquals(new BigDecimal("520"), entry.getEsiContractorShare(), "ESI Contractor Share Mismatch");

        // ESI Member: 16000 * 0.0075 = 120
        Assertions.assertEquals(new BigDecimal("120"), entry.getEsiMemberShare(), "ESI Member Share Mismatch");

        // Net Pay: 16000 - 1920 - 120 = 13960
        Assertions.assertEquals(new BigDecimal("13960"), entry.getNetPayable(), "Net Pay Mismatch for HL High Wage");
    }

    @Test
    public void testDemo_CasualLabour_Correction() {
        // Prepare CL Employee
        com.fci.automation.entity.Employee empOr = new com.fci.automation.entity.Employee();
        empOr.setCategory(com.fci.automation.entity.Employee.Category.CL);

        PayrollEntry entry = new PayrollEntry();
        entry.setEmployee(empOr);
        entry.setDaysWorked(10);
        entry.setWagesEarned(new BigDecimal("9999")); // User enters incorrect Wages manually
        entry.setAdvanceDeduction(new BigDecimal("1000")); // User enters 1000

        calculator.calculate(entry);

        // DEMO RESULT 1: Wages PRESERVED (User Request)
        Assertions.assertEquals(new BigDecimal("9999"), entry.getWagesEarned(),
                "Wages should be preserved (User Input)");

        // DEMO RESULT 2: Tax Calculated on STANDARD Wages (10 * 541 = 5410)
        // EPF Member: 12% of 5410 = 649
        Assertions.assertEquals(new BigDecimal("649"), entry.getEpfMemberShare(),
                "Tax should be based on Standard Wages (5410)");

        // DEMO RESULT 3: Net Pay ignores deductions (Entry View) and matches Input
        Assertions.assertEquals(new BigDecimal("9999"), entry.getNetPayable(),
                "Net Pay should be same as Input Wages (Entry View)");
    }

    @Test
    public void testCalculation_AneesPM() {
        // Data from Excel:
        // Wages: 11673
        // EPF Member: 1401
        // ESI Member: 88
        // ESI Contractor: 379
        // Net Pay: 10184 (Advance 0)

        PayrollEntry entry = new PayrollEntry();
        entry.setWagesEarned(new BigDecimal("11673"));
        entry.setAdvanceDeduction(BigDecimal.ZERO);

        calculator.calculate(entry);

        Assertions.assertEquals(new BigDecimal("1401"), entry.getEpfMemberShare(), "EPF Member Share Mismatch");
        Assertions.assertEquals(new BigDecimal("88"), entry.getEsiMemberShare(), "ESI Member Share Mismatch");

        // Check Net Pay
        // 11673 - 1401 - 88 - 0 = 10184
        Assertions.assertEquals(new BigDecimal("10184"), entry.getNetPayable(), "Net Pay Mismatch");

        // Note: ESI Contractor observation was 379. Let's see if our logic holds.
        // 11673 * 0.0325 = 379.3725. HALF_UP should be 379. Correct.
        // Wait, 379.37 rounds to 379. RoundingMode.HALF_UP (Legacy Math.round) rounds
        // to nearest neighbor. .37 is closer to .0 than 1.0.
        // So 379 is correct for HALF_UP.
        Assertions.assertEquals(new BigDecimal("379"), entry.getEsiContractorShare(), "ESI Contractor Share Mismatch");
    }
}
