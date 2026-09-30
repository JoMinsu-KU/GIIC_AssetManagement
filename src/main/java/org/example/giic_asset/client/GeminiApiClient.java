package org.example.giic_asset.client;

import lombok.extern.slf4j.Slf4j;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.gemini.GeminiRequest;
import org.example.giic_asset.dto.gemini.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GeminiApiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public GeminiApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> analyzeImage(String base64Image, String mimeType, String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new BaseException(ErrorCode.API_KEY_NOT_CONFIGURED);
        }

        GeminiRequest request = buildRequest(base64Image, mimeType, prompt);

        return webClient.post()
                .uri(apiUrl)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Gemini API client error: {}", response.statusCode());
                    return Mono.error(new BaseException(ErrorCode.BAD_REQUEST, "OCR 요청이 잘못되었습니다."));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("Gemini API server error: {}", response.statusCode());
                    return Mono.error(new BaseException(ErrorCode.EXTERNAL_API_ERROR));
                })
                .bodyToMono(GeminiResponse.class)
                .map(this::extractTextFromResponse)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable)
                        .onRetryExhaustedThrow((retryBackoffSpec,
                                retrySignal) -> new BaseException(ErrorCode.EXTERNAL_API_ERROR, "OCR 서비스 응답이 없습니다.")));
    }

    private GeminiRequest buildRequest(String base64Image, String mimeType, String prompt) {
        return GeminiRequest.builder()
                .contents(Collections.singletonList(
                        GeminiRequest.Content.builder()
                                .parts(List.of(
                                        GeminiRequest.Part.builder()
                                                .inline_data(GeminiRequest.InlineData.builder()
                                                        .mime_type(mimeType)
                                                        .data(base64Image)
                                                        .build())
                                                .build(),
                                        GeminiRequest.Part.builder()
                                                .text(prompt)
                                                .build()))
                                .build()))
                .generationConfig(GeminiRequest.GenerationConfig.builder()
                        .temperature(0.1)
                        .topK(1)
                        .topP(0.95)
                        .maxOutputTokens(4096)
                        .responseMimeType("application/json")
                        .build())
                .build();
    }

    private String extractTextFromResponse(GeminiResponse response) {
        if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new BaseException(ErrorCode.OCR_PARSING_ERROR, "AI 응답에 후보군이 없습니다.");
        }

        GeminiResponse.Candidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null
                || candidate.getContent().getParts().isEmpty()) {
            System.out.println(candidate);
            throw new BaseException(ErrorCode.OCR_PARSING_ERROR, "AI 응답에 내용이 없습니다.");
        }

        StringBuilder sb = new StringBuilder();
        for (GeminiResponse.Part part : candidate.getContent().getParts()) {
            if (part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return sb.toString();
    }
}
