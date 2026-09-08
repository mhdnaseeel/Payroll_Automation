package com.fci.automation.controller;

import com.fci.automation.dto.BankPayGenerateRequestDto;
import com.fci.automation.dto.BankPayImportResultDto;
import com.fci.automation.dto.BankPayRecordDto;
import com.fci.automation.service.BankPayService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bank-pay")
public class BankPayController {

    private final BankPayService bankPayService;

    public BankPayController(BankPayService bankPayService) {
        this.bankPayService = bankPayService;
    }

    @PostMapping("/import")
    public ResponseEntity<BankPayImportResultDto> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            BankPayImportResultDto result = bankPayService.parseAndMatchExcel(file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new RuntimeException("Error importing Bank Pay Excel: " + e.getMessage(), e);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateBankFile(@RequestBody BankPayGenerateRequestDto request) {
        String txt = bankPayService.generateBankFile(request);

        String type = request.getPaymentType() != null ? request.getPaymentType().toUpperCase() : "ALL";
        String filename = "sbi_bulk_payment.txt";
        if ("SAME_BANK".equals(type)) {
            filename = "sbi_same_bank_payment.txt";
        } else if ("OTHER_BANK".equals(type)) {
            filename = "sbi_other_bank_payment.txt";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(txt);
    }

    @PostMapping("/exception-report")
    public ResponseEntity<byte[]> getExceptionReport(@RequestBody List<BankPayRecordDto> records) {
        byte[] excel = bankPayService.generateExceptionReportExcel(records);
        String filename = "exception_report.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
