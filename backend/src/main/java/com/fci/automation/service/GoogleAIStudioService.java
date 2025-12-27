package com.fci.automation.service;

import com.fci.automation.dto.WorkSlipResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Service
public class GoogleAIStudioService {

  @Value("${google.ai.studio.api-key}")
  private String apiKey;

  private static final String GOOGLE_AI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GoogleAIStudioService.class);

  private final AzureOCRService azureOCRService;

  // The strict data structuring prompt for Google AI Studio
  private static final String SYSTEM_PROMPT = """
      SYSTEM ROLE:
      You are a STRICT data structuring and validation engine.
      Input is OCR JSON produced by Azure Document Intelligence.
      You must NOT re-OCR images.
      You must NOT guess or calculate.

      ------------------------------------------------
      INPUT GUARANTEE
      ------------------------------------------------
      You will receive:
      - Raw OCR text
      - Extracted key fields from Azure

      Assume OCR text is correct but unstructured.

      ------------------------------------------------
      SECTION CLASSIFICATION (STRICT)
      ------------------------------------------------
      Determine section ONLY from shed_or_remarks_text keywords:

      - Contains "ISSUE" → Section = ISSUE
      - Contains "RECEIPT" or "RECEIVED" → Section = RECEIPT
      - Contains "QC" or "CASUAL LABOUR" → Section = QC

      If:
      - Multiple match
      - Or unclear meaning

      Then:
      Section = "UNCONFIRMED"

      DO NOT guess.

      ------------------------------------------------
      STRUCTURING RULES
      ------------------------------------------------
      - Do NOT change values
      - Do NOT calculate totals
      - Do NOT fix spelling
      - Preserve NULL values
      - One slip = one section

      ------------------------------------------------
      FINAL OUTPUT (STRICT JSON ONLY)
      ------------------------------------------------

      {
        "status": "SUCCESS",
        "section": "ISSUE | RECEIPT | QC | UNCONFIRMED",
        "header": {
          "work_slip_no": "",
          "date_of_operation": "",
          "depot_name": "",
          "shed_or_remarks_text": ""
        },
        "quantities": {
          "total_bags_written": "",
          "bags_up_to_10_high": "",
          "bags_11_to_16_high": "",
          "bags_17_to_20_high": "",
          "bags_above_20_high": ""
        },
        "qc_details": {
          "labour_count": ""
        },
        "confidence": {
          "overall": "HIGH | MEDIUM | LOW"
        }
      }

      ------------------------------------------------
      FAILURE RULE
      ------------------------------------------------
      If required fields are missing or ambiguous:
      - Keep them NULL
      - Set confidence = LOW
      - Do NOT reject
      """;

  public GoogleAIStudioService(ObjectMapper objectMapper, AzureOCRService azureOCRService) {
    this.objectMapper = objectMapper;
    this.azureOCRService = azureOCRService;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
  }

  public WorkSlipResult extractWorkSlip(MultipartFile file) throws IOException {
    // Stage 1: Azure OCR
    String rawOcrText = azureOCRService.extractRawText(file);
    String ocrJsonInput = objectMapper.writeValueAsString(java.util.Collections.singletonMap("raw_text", rawOcrText));

    // Stage 2: Google AI Structuring
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode contentsArray = rootNode.putArray("contents");
    ObjectNode contentNode = contentsArray.addObject();
    ArrayNode partsArray = contentNode.putArray("parts");

    // Combine Prompt + OCR Input
    String finalUserPrompt = SYSTEM_PROMPT + "\n\n=== AZURE OCR OUTPUT ===\n" + ocrJsonInput;
    partsArray.addObject().put("text", finalUserPrompt);

    // NOTE: Image is NO LONGER sent to Gemini. Reliance is strictly on Azure Text.

    // Optional: Set temperature to 0 for strictness
    ObjectNode generationConfig = rootNode.putObject("generationConfig");
    generationConfig.put("temperature", 0.0);

    String jsonPayload = objectMapper.writeValueAsString(rootNode);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(GOOGLE_AI_URL))
        .header("Content-Type", "application/json")
        .header("X-goog-api-key", apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
        .timeout(Duration.ofMinutes(2))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new IOException("Google AI Studio API Error: " + response.statusCode() + " - " + response.body());
      }

      logger.info("Raw Google AI Response: {}", response.body());

      JsonNode responseRoot = objectMapper.readTree(response.body());

      // Check for candidates
      JsonNode candidates = responseRoot.path("candidates");
      if (candidates.isEmpty()) {
        // If blocked or no output
        if (responseRoot.has("promptFeedback")) {
          throw new IOException("Google AI Studio API Blocked: " + responseRoot.get("promptFeedback").toString());
        }
        throw new IOException("No candidates returned from Google AI Studio API");
      }

      // Get text from first candidate
      JsonNode parts = candidates.get(0).path("content").path("parts");
      StringBuilder extractedTextBuilder = new StringBuilder();
      if (parts.isArray()) {
        for (JsonNode part : parts) {
          if (part.has("text")) {
            extractedTextBuilder.append(part.get("text").asText());
          }
        }
      }

      String responseText = extractedTextBuilder.toString();
      logger.info("Extracted Text Block: {}", responseText);

      String jsonText = cleanJson(responseText);
      logger.debug("Cleaned JSON: {}", jsonText);

      // Save Debug Files
      saveDebugFiles(rawOcrText, jsonText);

      return objectMapper.readValue(jsonText, WorkSlipResult.class);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  private void saveDebugFiles(String azureText, String googleJson) {
    try {
      String timestamp = java.time.LocalDateTime.now()
          .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      java.nio.file.Path debugDir = java.nio.file.Paths.get("debug_runs", timestamp);
      java.nio.file.Files.createDirectories(debugDir);

      java.nio.file.Files.writeString(debugDir.resolve("azure_ocr.txt"), azureText);
      java.nio.file.Files.writeString(debugDir.resolve("google_result.json"), googleJson);

      logger.info("Saved debug files to: {}", debugDir.toAbsolutePath());
    } catch (IOException e) {
      logger.error("Failed to save debug files", e);
    }
  }

  private String cleanJson(String responseText) {
    if (responseText == null)
      return "{}";
    String content = responseText.trim();
    if (content.startsWith("```json")) {
      content = content.substring(7);
    } else if (content.startsWith("```")) {
      content = content.substring(3);
    }
    if (content.endsWith("```")) {
      content = content.substring(0, content.length() - 3);
    }
    return content.trim();
  }
}
