package com.fci.automation.service;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Service
public class AzureOCRService {

    private static final Logger logger = LoggerFactory.getLogger(AzureOCRService.class);
    private final DocumentAnalysisClient client;

    public AzureOCRService(
            @Value("${azure.form.recognizer.endpoint}") String endpoint,
            @Value("${azure.form.recognizer.key}") String key) {
        this.client = new DocumentAnalysisClientBuilder()
                .credential(new AzureKeyCredential(key))
                .endpoint(endpoint)
                .buildClient();
    }

    public String extractRawText(MultipartFile file) throws IOException {
        logger.info("Starting Azure OCR extraction for file: {}", file.getOriginalFilename());

        try {
            BinaryData data = BinaryData.fromStream(file.getInputStream(), file.getSize());
            SyncPoller<OperationResult, AnalyzeResult> analyzeDocumentPoller = client
                    .beginAnalyzeDocument("prebuilt-layout", data);

            AnalyzeResult result = analyzeDocumentPoller.getFinalResult();
            String content = result.getContent();

            logger.info("Azure OCR completed. Content length: {}", content.length());
            logger.debug("Azure Raw Output: {}", content);

            return content;
        } catch (Exception e) {
            logger.error("Azure OCR failed", e);
            throw new IOException("Azure OCR Extraction Failed: " + e.getMessage(), e);
        }
    }
}
