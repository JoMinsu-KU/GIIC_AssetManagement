package org.example.giic_asset.service;

import org.example.giic_asset.dto.ocr.OcrResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class GeminiOcrServiceTest {

    @Autowired
    private GeminiOcrService geminiOcrService;

    @BeforeAll
    static void setup() {
        // Load .env file if it exists and set system properties
        try {
            if (Files.exists(Paths.get(".env"))) {
                try (Stream<String> lines = Files.lines(Paths.get(".env"))) {
                    lines.forEach(line -> {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            if (key.equals("GEMINI_API_KEY")) {
                                System.setProperty("GEMINI_API_KEY", value);
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read .env file: " + e.getMessage());
        }
    }

    @Test
    void testAnalyzeImage() {
        // 1x1 Red Pixel PNG Base64
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=";
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                imageBytes);

        System.out.println("Starting OCR analysis test...");

        try {
            OcrResponse response = geminiOcrService.processOcr(file);

            assertNotNull(response);
            System.out.println("OCR Test Result: " + response);
            // System.out.println("Extracted Fields: " + response.getExtractedFields());
        } catch (Exception e) {
            System.err.println("OCR Test Failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testAnalyzeImageWithRealFile() throws IOException {
        // 실제 이미지 파일 읽기
        Path imagePath = Paths.get("uploads/5f0d82ff-42ab-4a2b-a6ab-46ea0e946b53_KakaoTalk_20260128_163859930.jpg");

        if (!Files.exists(imagePath)) {
            System.out.println("테스트 이미지 파일이 존재하지 않습니다: " + imagePath.toAbsolutePath());
            return;
        }

        // 파일을 Base64로 인코딩
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = "image/jpeg";

        System.out.println("이미지 파일 크기: " + imageBytes.length + " bytes");
        System.out.println("Base64 길이: " + base64Image.length());

        // OCR 호출
        OcrResponse response = geminiOcrService.analyzeImage(base64Image, mimeType);

        // 결과 출력
        System.out.println("===== OCR 결과 =====");
        System.out.println("Response: " + response);
        if (response != null) {
            // response의 각 필드 출력
            System.out.println("추출된 데이터: " + response.toString());
        }

        assertNotNull(response);
    }
}
