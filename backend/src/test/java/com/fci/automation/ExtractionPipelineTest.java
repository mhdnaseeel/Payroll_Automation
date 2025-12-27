package com.fci.automation;

import com.fci.automation.dto.WorkSlipResult;
import com.fci.automation.service.GoogleAIStudioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExtractionPipelineTest {

    private static final Logger logger = LoggerFactory.getLogger(ExtractionPipelineTest.class);

    @Autowired
    private GoogleAIStudioService googleAIStudioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testHybridExtractionPipeline() throws Exception {
        logger.info("Starting Hybrid Extraction Test...");

        // Load sample image
        ClassPathResource resource = new ClassPathResource("workslip_sample.jpg");
        assertTrue(resource.exists(), "Test image not found in resources!");

        try (InputStream is = resource.getInputStream()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "workslip_sample.jpg",
                    "image/jpeg",
                    is);

            // Execute Pipeline
            WorkSlipResult result = googleAIStudioService.extractWorkSlip(file);

            // Print Result
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            logger.info("FINAL EXTRACTION RESULT:\n{}", jsonOutput);

            // Basic Assertions
            assertNotNull(result, "Result should not be null");
            assertNotNull(result.getStatus(), "Status should not be null");

            if ("SUCCESS".equals(result.getStatus())) {
                assertNotNull(result.getHeader(), "Header should be present");
                assertNotNull(result.getQuantities(), "Quantities should be present");
                logger.info("Test Passed: Extraction was SUCCESSFUL");
            } else {
                logger.warn("Test Finished with Status: {}", result.getStatus());
            }
        }
    }
}
