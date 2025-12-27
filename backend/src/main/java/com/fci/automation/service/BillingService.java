package com.fci.automation.service;

import com.fci.automation.dto.IssueSlipDTO;
import com.fci.automation.dto.WorkSlipResult;
import com.fci.automation.entity.WorkSlip;
import com.fci.automation.repository.WorkSlipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingService {

    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WorkSlipRepository workSlipRepository;
    private final GoogleAIStudioService googleAIStudioService;

    public BillingService(WorkSlipRepository workSlipRepository, GoogleAIStudioService googleAIStudioService) {
        this.workSlipRepository = workSlipRepository;
        this.googleAIStudioService = googleAIStudioService;
    }

    /**
     * EXTRACT ISSUE DATA (Google AI Direct)
     */
    public List<IssueSlipDTO> extractIssueData(List<MultipartFile> files) {
        List<IssueSlipDTO> extractedSlips = new ArrayList<>();
        int siCounter = 1;

        for (MultipartFile file : files) {
            IssueSlipDTO dto = new IssueSlipDTO();
            dto.setSiNo(String.valueOf(siCounter++));

            try {
                // Call Google AI
                WorkSlipResult result = googleAIStudioService.extractWorkSlip(file);

                // Map Result to DTO
                mapResultToDTO(dto, result);

            } catch (Exception e) {
                logger.error("Google AI Extraction Failed for file: " + file.getOriginalFilename(), e);
                dto.setStatus("NEEDS_VERIFICATION");
                dto.setWarningMessage("Extraction Error: " + e.getMessage());
            }

            extractedSlips.add(dto);
        }
        return extractedSlips;
    }

    private void mapResultToDTO(IssueSlipDTO dto, WorkSlipResult result) {
        if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
            dto.setStatus("EXTRACTED");
            dto.setConfidenceScore(0.95);
            dto.setWarningMessage(null);

            // Map Header
            if (result.getHeader() != null) {
                dto.setSlipNumber(result.getHeader().getWorkSlipNo());

                String dateStr = result.getHeader().getDateOfOperation();
                if (dateStr != null && !dateStr.equalsIgnoreCase("NULL")) {
                    try {
                        dto.setEntryDate(LocalDate.parse(dateStr, DATE_FORMATTER));
                    } catch (Exception e) {
                        logger.warn("Date parse error: {}", dateStr);
                        // Leave null or try other formats if needed
                    }
                }
            }

            // Map Quantities
            if (result.getQuantities() != null) {
                String totalStr = result.getQuantities().getTotalBagsWritten();
                if (totalStr != null && !totalStr.equalsIgnoreCase("NULL")) {
                    try {
                        dto.setTotalBags(Integer.parseInt(totalStr));
                    } catch (NumberFormatException e) {
                        logger.warn("Total bags parse error: {}", totalStr);
                    }
                }
            }

            // Clause/Part are not determining factors in the new prompt, but we can try to
            // extract from shed/remarks if needed
            // For now leaving them null as per stricter prompt instructions ("NO MORE NO
            // LESS")
            dto.setClause(null);
            dto.setPart(null);

        } else {
            dto.setStatus("REJECTED");
            dto.setConfidenceScore(0.0);
            dto.setWarningMessage(result.getStatus() + ": Not a valid Work Slip");
        }
    }

    /**
     * SAVE ISSUE DATA (Step 2 Completion)
     * STRICT VALIDATION: No unverified rows. Duplicates blocked.
     */
    @Transactional
    public void saveIssueData(List<IssueSlipDTO> dtos) {
        for (IssueSlipDTO dto : dtos) {
            // 1. BLOCKING VALIDATION: Status Check
            if ("NEEDS_VERIFICATION".equals(dto.getStatus())) {
                throw new RuntimeException("Validation Failed: Row SI No " + dto.getSiNo() + " requires verification.");
            }

            // 2. BLOCKING VALIDATION: Missing Fields
            if (dto.getSlipNumber() == null || dto.getSlipNumber().isBlank()) {
                throw new RuntimeException(
                        "Validation Failed: Row SI No " + dto.getSiNo() + " is missing Work Slip No.");
            }
            if (dto.getEntryDate() == null) {
                throw new RuntimeException("Validation Failed: Row SI No " + dto.getSiNo() + " is missing Date.");
            }
            if (dto.getTotalBags() == null || dto.getTotalBags() <= 0) {
                throw new RuntimeException(
                        "Validation Failed: Row SI No " + dto.getSiNo() + " has invalid Bags count.");
            }

            // 3. DUPLICATE CHECK (Strict: Slip No + Category)
            if (workSlipRepository.existsBySlipNumberAndCategory(dto.getSlipNumber(),
                    WorkSlip.WorkSlipCategory.ISSUE)) {
                throw new RuntimeException(
                        "Duplicate work slip detected: Work Slip No " + dto.getSlipNumber() + ". Please review.");
            }

            // 4. SAVE
            WorkSlip slip = new WorkSlip();
            slip.setCategory(WorkSlip.WorkSlipCategory.ISSUE);
            slip.setSlipNumber(dto.getSlipNumber());
            slip.setEntryDate(dto.getEntryDate());
            slip.setIssueTotalBags(dto.getTotalBags());
            slip.setImagePath("placeholder_path"); // Simplified for Phase 16

            workSlipRepository.save(slip);
        }
    }

    public List<WorkSlip> getSavedIssueSlips() {
        // Retrieve only ISSUE slips, ordered by date or slip number
        // Assuming we want all for now, or filter by current active month (TODO)
        return workSlipRepository.findByCategoryAndEntryDateBetween(
                WorkSlip.WorkSlipCategory.ISSUE,
                LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31) // Placeholder range
        );
    }
}
