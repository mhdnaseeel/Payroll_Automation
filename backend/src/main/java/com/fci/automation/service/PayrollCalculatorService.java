package com.fci.automation.service;

import com.fci.automation.entity.PayrollEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PayrollCalculatorService {

    // Constants from Analysis
    private static final BigDecimal EPF_RATE = new BigDecimal("0.12"); // 12%
    private static final BigDecimal ESI_MEMBER_RATE = new BigDecimal("0.0075"); // 0.75%
    private static final BigDecimal ESI_CONTRACTOR_RATE = new BigDecimal("0.0325"); // 3.25%
    private static final BigDecimal BONUS_RATE = new BigDecimal("0.0833"); // 8.33%

    private static final BigDecimal EPF_CAP_LIMIT = new BigDecimal("15000");
    private static final BigDecimal EPF_MAX_SHARE = new BigDecimal("1800");

    public void calculate(PayrollEntry entry) {
        // Validation moved to validator

        BigDecimal inputWages = entry.getWagesEarned();
        BigDecimal taxBaseWages; // Used for calculating EPF/ESI
        BigDecimal displayWages; // Used for Net Pay / UI

        // 1. Determine Wages (Input vs Standard)
        if (entry.getEmployee() != null
                && entry.getEmployee().getCategory() == com.fci.automation.entity.Employee.Category.CL) {
            // Casual Labour:
            // Standard (Tax Base) = Days * 541
            int days = entry.getDaysWorked() != null ? entry.getDaysWorked() : 0;
            BigDecimal standardWages = new BigDecimal(days).multiply(new BigDecimal("541"));

            taxBaseWages = standardWages;

            // Display/Input Logic: Preserve User Input if exists
            if (inputWages == null) {
                inputWages = standardWages;
                entry.setWagesEarned(inputWages); // Defaulting
            }
            displayWages = inputWages;

        } else {
            // Regular/Head Loaders: Tax Base IS the Input
            if (inputWages == null)
                inputWages = BigDecimal.ZERO;
            taxBaseWages = inputWages;
            displayWages = inputWages;
        }

        // 2. EPF Calculations (Use Tax Base)
        BigDecimal epfMember = calculateEpfMember(taxBaseWages);
        BigDecimal epfContractor = calculateEpfContractor(taxBaseWages);

        entry.setEpfMemberShare(epfMember);
        entry.setEpfContractorShare(epfContractor);

        // 3. ESI Calculations (Use Tax Base)
        BigDecimal esiMember = calculateEsiMember(taxBaseWages);
        BigDecimal esiContractor = calculateEsiContractor(taxBaseWages);

        entry.setEsiMemberShare(esiMember);
        entry.setEsiContractorShare(esiContractor);

        // 4. Bonus (Standard 8.33% on Tax Base?)
        // Start Step 2 says: "Wages Earned = 'Master Data'!G5 * 541" for CL.
        // It implies EVERYTHING is based on that.
        BigDecimal bonus = taxBaseWages.multiply(BONUS_RATE).setScale(0, RoundingMode.HALF_UP);
        entry.setBonusShare(bonus);

        // 5. Net Pay (Use Display Wages)
        // Logic: Net = Wages - EPF Member - ESI Member - Advance
        BigDecimal deduction = entry.getAdvanceDeduction() != null ? entry.getAdvanceDeduction() : BigDecimal.ZERO;
        BigDecimal totalDeduction = epfMember.add(esiMember).add(deduction);

        if (entry.getEmployee() != null
                && entry.getEmployee().getCategory() == com.fci.automation.entity.Employee.Category.CL) {
            // User Requirement: CL Net Pay in Entry Screen must equal Input Wages (No
            // visible deduction).
            entry.setNetPayable(displayWages);
        } else {
            // Regular (HL): Standard Net Pay formula
            entry.setNetPayable(displayWages.subtract(totalDeduction));
        }
    }

    // --- LOGIC HELPERS ---

    private BigDecimal calculateEpfContractor(BigDecimal wages) {
        if (wages.compareTo(EPF_CAP_LIMIT) > 0) {
            return EPF_MAX_SHARE;
        }
        return wages.multiply(EPF_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEpfMember(BigDecimal wages) {
        // Member Contribution = ROUND(H5 * 12%, 0) - No Cap mentioned in user request
        // for Member
        return wages.multiply(EPF_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEsiContractor(BigDecimal wages) {
        return wages.multiply(ESI_CONTRACTOR_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEsiMember(BigDecimal wages) {
        return wages.multiply(ESI_MEMBER_RATE).setScale(0, RoundingMode.HALF_UP);
    }
}
