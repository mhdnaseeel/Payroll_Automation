package com.fci.automation.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankPayImportResultDto {
    private List<BankPayRecordDto> records;
    private int totalRecords;
    private int totalSuccess;
    private int totalFailed;
    private int sameBankCount;
    private BigDecimal sameBankAmount;
    private int otherBankCount;
    private BigDecimal otherBankAmount;
    private BigDecimal totalAmount;
}
