package org.example.giic_asset.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.client.GeminiApiClient;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import reactor.core.publisher.Mono;
import org.example.giic_asset.dto.ocr.OcrResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiOcrService {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    private static final String OCR_PROMPT = """
            You are a STRICT OCR extraction engine.

             You must analyze the provided image (equipment label, nameplate, or sticker) and extract asset-related information.

             ⚠️ ABSOLUTE OUTPUT RULES (VIOLATION NOT ALLOWED):
             - Your response MUST be a single valid JSON object
             - Do NOT include any explanations, comments, apologies, markdown, or extra text
             - Do NOT wrap the JSON in code blocks
             - Do NOT add line breaks before or after the JSON
             - If any rule is violated, the response is considered INVALID

             TASK:
             Extract ONLY information that is clearly and explicitly visible in the image.

             FIELDS TO EXTRACT:
             1. asset_no      : Asset number, serial number, asset tag, or product code
             2. name          : Product category or item name (use Korean when appropriate)
             3. managing_dept : managing department
             4. purchase_date : Purchase date if explicitly visible
             5. spec          : Technical specifications (CPU, RAM, storage, voltage, size, etc.)

             EXTRACTION RULES:
             - Do NOT infer, guess, translate, or normalize information
             - Do NOT combine multiple fields into one
             - If a value is NOT clearly visible, set the field value to null
             - Preserve original capitalization, symbols, and formatting as shown in the image
             - Use string values only or null (no arrays, no objects)

             OUTPUT FORMAT (MUST MATCH EXACTLY):
             {
               "asset_no": "extracted value or null",
               "name": "extracted value or null",
               "managing_dept": "extracted value or null",
               "purchase_date": "extracted value or null",
               "spec": "extracted value or null"
             }

             REMINDER:
             The response MUST contain ONLY the JSON object above.
             Any text outside the JSON is STRICTLY FORBIDDEN.

            """;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public OcrResponse processOcr(MultipartFile file) {
        validateFile(file);

        String base64Image = encodeToBase64(file);
        String mimeType = file.getContentType();
        if (mimeType == null)
            mimeType = "image/jpeg"; // Default fallback

        return analyzeImage(base64Image, mimeType);
    }

    public OcrResponse analyzeImage(String base64Image, String mimeType) {
        // Block for synchronous execution as per design
        String jsonResponse = geminiApiClient
                .analyzeImage(base64Image, mimeType, OCR_PROMPT)
                .block();
        System.out.println(jsonResponse);
        OcrResponse response = parseOcrResponse(jsonResponse);
        response.setConfidence(calculateConfidence(response));

        log.info("OCR processing completed: assetNo={}, name={}", response.getAssetNo(), response.getName());
        return response;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "파일이 비어있습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(ErrorCode.IMAGE_TOO_LARGE);
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "파일명이 없습니다.");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_FORMAT);
        }
    }

    private String encodeToBase64(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("Failed to encode image to Base64", e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 처리 중 오류가 발생했습니다.");
        }
    }

    private OcrResponse parseOcrResponse(String jsonResponse) {
        try {
            // Remove code blocks if present (Gemini sometimes returns ```json ... ```)
            String cleanedJson = jsonResponse.replace("```json", "").replace("```", "").trim();

            JsonNode rootNode = objectMapper.readTree(cleanedJson);

            OcrResponse.OcrResponseBuilder builder = OcrResponse.builder();
            builder.rawText(cleanedJson);

            String assetNo = getFieldValue(rootNode, "asset_no");
            if (assetNo != null) {
                builder.assetNo(assetNo);
            }

            String name = getFieldValue(rootNode, "name");
            if (name != null) {
                builder.name(name);
            }

            String managingDept = getFieldValue(rootNode, "managing_dept");
            if (managingDept != null) {
                builder.managingDept(managingDept);
            }

            String purchaseDate = getFieldValue(rootNode, "purchase_date");
            if (purchaseDate != null) {
                builder.purchaseDate(purchaseDate);
            }

            String spec = getFieldValue(rootNode, "spec");
            if (spec != null) {
                builder.spec(spec);
            }

            return builder.build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse OCR response: {}", jsonResponse, e);
            throw new BaseException(ErrorCode.OCR_PARSING_ERROR);
        }
    }

    private String getFieldValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull() && fieldNode.isTextual()) {
            String value = fieldNode.asText().trim();
            return value.isEmpty() || "null".equalsIgnoreCase(value) ? null : value;
        }
        return null;
    }

    private Double calculateConfidence(OcrResponse response) {
        int totalFields = 5;
        int extractedCount = 0;

        if (response.getAssetNo() != null)
            extractedCount++;
        if (response.getName() != null)
            extractedCount++;
        if (response.getManagingDept() != null)
            extractedCount++;
        if (response.getPurchaseDate() != null)
            extractedCount++;
        if (response.getSpec() != null)
            extractedCount++;

        double baseConfidence = (double) extractedCount / totalFields;

        int criticalFields = 0;
        if (response.getAssetNo() != null)
            criticalFields++;
        if (response.getName() != null)
            criticalFields++;

        // Give extra weight to critical fields (AssetNo, Name)
        double criticalWeight = (double) criticalFields / 2 * 0.3;

        return Math.min(baseConfidence + criticalWeight, 1.0);
    }
}
