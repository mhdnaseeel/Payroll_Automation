package com.fci.automation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueSlipDTO {
    private String siNo; // For UI reference only based on Master Prompt
    private LocalDate entryDate;
    private String slipNumber;
    private Integer totalBags;
    private String clause;
    private String part;
    private String status; // "EXTRACTED", "NEEDS_VERIFICATION", "EDITED"
    private String warningMessage; // For duplicate/missing field warnings
    private Double confidenceScore; // 0.0 to 1.0
}
