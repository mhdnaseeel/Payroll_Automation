package com.fci.automation.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankPayGenerateRequestDto {
    private List<BankPayRecordDto> records;
    private String paymentDate; // e.g. "2026-08-05" or "05/08/2026"
    private String companyAccount;
    private String branchCode;
    private String companyName;
    private String paymentType; // "SAME_BANK", "OTHER_BANK", or "ALL"
}
