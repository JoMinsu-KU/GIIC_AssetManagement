package org.example.giic_asset.dto.gemini;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GeminiResponse {
    private List<Candidate> candidates;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Candidate {
        private Content content;
        private String finishReason;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part {
        private String text;
    }
}
