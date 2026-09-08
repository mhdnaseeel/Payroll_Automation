package com.fci.automation.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankPayRecordDto {
    private String employeeId;
    private String employeeName;
    private String accountNumber;
    private String ifscCode;
    private BigDecimal amount;
    private String status; // "READY", "NOT_FOUND", "INVALID"
    private String reason;
    private String bankCategory; // "SAME_BANK", "OTHER_BANK"
}
