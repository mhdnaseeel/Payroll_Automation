package com.fci.automation.controller;

import com.fci.automation.dto.WorkSlipResult;
import com.fci.automation.service.GoogleAIStudioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@RestController
@RequestMapping("/api/extract")
@CrossOrigin(originPatterns = "*") // Changed to support allowCredentials if needed
public class DocumentExtractionController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentExtractionController.class);

    private final GoogleAIStudioService extractionService;

    public DocumentExtractionController(GoogleAIStudioService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping("/work-slip")
    public ResponseEntity<WorkSlipResult> extractWorkSlip(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            logger.info("Received work slip extraction request for file: {}", file.getOriginalFilename());
            WorkSlipResult result = extractionService.extractWorkSlip(file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("Error reading file", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Error during extraction", e);
            // In case of parsing error or API error, we might want to return details,
            // but for now 500 is sufficient as per standard practices unless specific error
            // handling is requested.
            return ResponseEntity.internalServerError().build();
        }
    }
}
