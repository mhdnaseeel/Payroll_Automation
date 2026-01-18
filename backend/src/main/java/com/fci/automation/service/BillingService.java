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
    private final MistralAIService mistralAIService;

    public BillingService(WorkSlipRepository workSlipRepository, MistralAIService mistralAIService) {
        this.workSlipRepository = workSlipRepository;
        this.mistralAIService = mistralAIService;
    }

    /**
     * EXTRACT ISSUE DATA (Mistral AI Direct)
     */
    public List<IssueSlipDTO> extractIssueData(List<MultipartFile> files) {
        List<IssueSlipDTO> extractedSlips = new ArrayList<>();
        int siCounter = 1;

        for (MultipartFile file : files) {
            IssueSlipDTO dto = new IssueSlipDTO();
            dto.setSiNo(String.valueOf(siCounter++));

            try {
                // Call Mistral AI
                WorkSlipResult result = mistralAIService.extractWorkSlip(file);

                // Map Result to DTO
                mapResultToDTO(dto, result);

            } catch (Exception e) {
                logger.error("Mistral AI Extraction Failed for file: " + file.getOriginalFilename(), e);
                dto.setStatus("NEEDS_VERIFICATION");
                dto.setWarningMessage("Extraction Error: " + e.getMessage());
            }

            extractedSlips.add(dto);
        }
        return extractedSlips;
    }

    private void mapResultToDTO(IssueSlipDTO dto, WorkSlipResult result) {
        // Validation: Check for "Issue" OR presence of specific fields
        boolean isIssue = "Issue".equalsIgnoreCase(result.getIssue());
        boolean hasSlipNo = result.getWorkSlipNo() != null && !result.getWorkSlipNo().isEmpty();

        if (!isIssue && !hasSlipNo) {
            // Strictly reject only if BOTH indicators are missing
            dto.setStatus("REJECTED");
            dto.setWarningMessage("Document not identified as 'Issue' slip.");
            return;
        }

        // Map Fields
        dto.setSlipNumber(result.getWorkSlipNo());

        // Date Mapping
        String dateStr = result.getDate();
        if (dateStr != null && !dateStr.equalsIgnoreCase("null")) {
            try {
                // Normalize date string: replace separators with slashes
                String cleanDate = dateStr.replace(".", "/").replace(" ", "/").replace("-", "/");

                // Remove non-numeric/slash characters
                cleanDate = cleanDate.replaceAll("[^0-9/]", "");

                // Fix single digit parts? e.g. "11/2/5" -> "11/02/05"
                String[] parts = cleanDate.split("/");
                if (parts.length == 3) {
                    // Normalize Day
                    if (parts[0].length() == 1)
                        parts[0] = "0" + parts[0];
                    // Normalize Month
                    if (parts[1].length() == 1)
                        parts[1] = "0" + parts[1];
                    // Normalize Year (Handle 1, 2, or 3 digits)
                    String yearPart = parts[2];
                    if (yearPart.length() == 1) {
                        parts[2] = "200" + yearPart;
                    } else if (yearPart.length() == 2) {
                        parts[2] = "20" + yearPart;
                    } else if (yearPart.length() == 3) {
                        // Handle noise like "125" -> "25" -> "2025"
                        parts[2] = "20" + yearPart.substring(1);
                    }

                    cleanDate = parts[0] + "/" + parts[1] + "/" + parts[2];
                }

                // Use robust formatter handling yyyy
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                dto.setEntryDate(LocalDate.parse(cleanDate, formatter));
            } catch (Exception e) {
                logger.warn("Date parse error: {} (original: {})", e.getMessage(), dateStr);
                dto.setWarningMessage("Check Date: " + dateStr);
            }
        }

        // Bags Mapping
        dto.setTotalBags(result.getBags());

        // Final Status Determination
        if (dto.getSlipNumber() != null && dto.getEntryDate() != null && dto.getTotalBags() != null) {
            dto.setStatus("EXTRACTED");
            dto.setConfidenceScore(1.0);
            dto.setWarningMessage(null);
        } else {
            dto.setStatus("NEEDS_VERIFICATION");
            // Build descriptive warning
            List<String> missing = new ArrayList<>();
            if (dto.getSlipNumber() == null)
                missing.add("Slip No");
            if (dto.getEntryDate() == null)
                missing.add("Date");
            if (dto.getTotalBags() == null)
                missing.add("Bags");

            String existingWarn = dto.getWarningMessage();
            String newWarn = "Missing: " + String.join(", ", missing);

            dto.setWarningMessage(existingWarn == null ? newWarn : existingWarn + "; " + newWarn);
        }

        dto.setClause(null);
        dto.setPart(null);
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
