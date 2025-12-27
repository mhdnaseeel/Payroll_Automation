package com.fci.automation.controller;

import com.fci.automation.dto.IssueSlipDTO;
import com.fci.automation.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BillingController.class);

    @Autowired
    private BillingService billingService;

    @PostMapping(value = "/issue/extract", consumes = "multipart/form-data")
    public List<IssueSlipDTO> extractIssueData(@RequestParam("files") List<MultipartFile> files) {
        try {
            logger.info("Received extraction request for {} files.", files.size());
            return billingService.extractIssueData(files);
        } catch (Exception e) {
            e.printStackTrace();
            // Return a single error DTO so frontend displays it in the table instead of
            // crashing
            IssueSlipDTO errorDto = new IssueSlipDTO();
            errorDto.setSiNo("Error");
            errorDto.setStatus("NEEDS_VERIFICATION");
            errorDto.setWarningMessage("System Error: " + e.getMessage());
            return List.of(errorDto);
        }
    }

    @PostMapping("/issue/save")
    public void saveIssueData(@RequestBody List<IssueSlipDTO> slips) {
        billingService.saveIssueData(slips);
    }

    @GetMapping("/issue/list")
    public List<com.fci.automation.entity.WorkSlip> getSavedIssueSlips() {
        return billingService.getSavedIssueSlips();
    }
}
