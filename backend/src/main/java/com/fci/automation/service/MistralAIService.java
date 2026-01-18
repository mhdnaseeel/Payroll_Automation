package com.fci.automation.service;

import com.fci.automation.dto.WorkSlipResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class MistralAIService {

    private static final Logger logger = LoggerFactory.getLogger(MistralAIService.class);

    @Value("${mistral.api.key}")
    private String apiKey;

    private static final String MISTRAL_API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final String MODEL = "mistral-small-latest";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TesseractOCRService tesseractOCRService;

    // The strict data structuring prompt adapted for Mistral
    private static final String SYSTEM_PROMPT = """
            You are a document OCR data-extraction system.

            You will receive OCR output (text + bounding boxes) produced by Tesseract OCR from images of
            “Food Corporation of India (FCI) – Work Slip” documents.

            All documents share the same printed layout.
            Handwritten values may vary.

            Your task is to extract ONLY the following four fields from EACH document:

            1. work_slip_no
            2. issue
            3. date
            4. bags

            Ignore all other content completely.

            ────────────────────────
            EXTRACTION RULES
            ────────────────────────

            WORK SLIP NO
            • Location: top-left region of the document
            • Type: printed numeric value
            • Format: 3–4 digits
            • Selection rule:
              – choose the left-most numeric token near the “WORK SLIP” header
              – ignore dates and bag quantities

            ISSUE
            • Location: below the depot name
            • Type: handwritten text
            • Expected value: “Issue”
            • Matching rule:
              – accept minor OCR distortions (Issue, lssue, iss ue)
              – if detected → return "Issue"
              – if not detected → return null

            DATE
            • Location: top-right region near the word “Date”
            • Type: handwritten
            • Accepted formats:
              – DD/MM/YY
              – DD/MM/YYYY
            • Selection rule:
              – extract the date closest to the “Date” label
              – do not infer or reformat
              – return exactly as written

            BAGS
            • Location: column titled “No. of bags / quantity handled”
            • Type: handwritten integers

            • Rules:
              – if only one number exists → use it
              – if multiple numbers exist:
                  ▸ use the final total (usually underlined or written last)
                  ▸ verify that sub-values sum to the final total
              – if totals conflict or are unclear → return null
              – bag value must be a positive integer

            ────────────────────────
            OUTPUT FORMAT (MANDATORY)
            ────────────────────────

            Return ONLY valid JSON.
            No explanations.
            No extra text.

            {
              "work_slip_no": "____",
              "issue": "Issue",
              "date": "____",
              "bags": ____
            }

            ────────────────────────
            CONSTRAINTS
            ────────────────────────
            • Process each document independently
            • Never guess missing values
            • Never hallucinate numbers
            • Accuracy is more important than completeness
            • If a field is unreadable, return null for that field
            """;

    public MistralAIService(ObjectMapper objectMapper, TesseractOCRService tesseractOCRService) {
        this.objectMapper = objectMapper;
        this.tesseractOCRService = tesseractOCRService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public WorkSlipResult extractWorkSlip(MultipartFile file) throws IOException {
        // Stage 1: Tesseract OCR
        String rawOcrText = tesseractOCRService.extractRawText(file);

        // Stage 2: Mistral AI Structuring
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("model", MODEL);
        rootNode.put("temperature", 0.0);

        ArrayNode messagesArray = rootNode.putArray("messages");

        // System Message
        ObjectNode systemMessage = messagesArray.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);

        // User Message
        ObjectNode userMessage = messagesArray.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", "=== TESSERACT OCR OUTPUT ===\n" + rawOcrText);

        ObjectNode responseFormat = rootNode.putObject("response_format");
        responseFormat.put("type", "json_object");

        String jsonPayload = objectMapper.writeValueAsString(rootNode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MISTRAL_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofMinutes(2))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Mistral AI API Error: " + response.statusCode() + " - " + response.body());
            }

            logger.info("Raw Mistral AI Response: {}", response.body());

            JsonNode responseRoot = objectMapper.readTree(response.body());
            JsonNode choices = responseRoot.path("choices");

            if (choices.isEmpty()) {
                throw new IOException("No choices returned from Mistral AI API");
            }

            String content = choices.get(0).path("message").path("content").asText();
            logger.info("Extracted JSON Block: {}", content);

            // Save Debug Files
            saveDebugFiles(rawOcrText, content);

            return objectMapper.readValue(content, WorkSlipResult.class);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    private void saveDebugFiles(String ocrText, String mistralJson) {
        try {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            java.nio.file.Path debugDir = java.nio.file.Paths.get("debug_runs", timestamp);
            java.nio.file.Files.createDirectories(debugDir);

            java.nio.file.Files.writeString(debugDir.resolve("tesseract_ocr.txt"), ocrText);
            java.nio.file.Files.writeString(debugDir.resolve("mistral_result.json"), mistralJson);

            logger.info("Saved debug files to: {}", debugDir.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save debug files", e);
        }
    }
}
