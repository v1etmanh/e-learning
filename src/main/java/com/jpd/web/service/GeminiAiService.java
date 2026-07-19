package com.jpd.web.service;

import com.jpd.web.exception.UsageLimitExceededException;
import com.jpd.web.service.utils.SubscriptionTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeminiAiService {

    // Prefer the environment variable in deployed environments. Keep the
    // existing Spring property as a local-development fallback.
    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private FeedbackUsageService feedbackUsageService;

    /**
     * Generate feedback với kiểm tra usage limit
     * @param userId ID người dùng
     * @param tier Gói subscription của user
     * @param question Câu hỏi
     * @param answer Câu trả lời
     * @return Feedback từ AI
     */
    public String generateFeedback(String userId, SubscriptionTier tier, String question, String answer) {
        // Kiểm tra và ghi nhận usage (sẽ throw exception nếu vượt limit)
        feedbackUsageService.checkAndRecordUsage(userId, tier);

        // Tạo prompt
        StringBuilder sb = new StringBuilder();
        sb.append("You are a multilingual language tutor.\n");
        sb.append("Your task is to analyze the learner's answer in ANY language.\n");
        sb.append("Feedback must be written in clear and natural Vietnamese.\n");
        sb.append("Feedback length: 1–3 sentences, no lists, no JSON, no extra formatting.\n\n");
        sb.append("The feedback must include:\n");
        sb.append("1. Confirmation: nói câu trả lời đúng hay sai.\n");
        sb.append("2. Explanation: giải thích ngắn gọn về ngữ pháp hoặc từ vựng.\n");
        sb.append("Câu hỏi: \"").append(escapeForPrompt(question)).append("\"\n");
        sb.append("Câu trả lời: \"").append(escapeForPrompt(answer)).append("\"\n");

        return generateContent(sb.toString());
    }

    public String generateContent(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("candidates")) {
            var candidates = (java.util.List<Map<String, Object>>) body.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }
        }
        return "No response from Gemini";
    }

    public String generateContentWithImage(String prompt, String base64Image, String mimeType) {
        String enhancedPrompt = prompt + "\n\nIMPORTANT: Your response must start with [ and end with ] only. Nothing else.";

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", enhancedPrompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Image
                                ))
                        })
                },
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "topK", 1,
                        "topP", 0.1,
                        "maxOutputTokens", 500
                )
        );

        return sendRequest(requestBody);
    }

    private String sendRequest(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("candidates")) {
            var candidates = (java.util.List<Map<String, Object>>) body.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }
        }
        return "No response from Gemini";
    }

    private String escapeForPrompt(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
