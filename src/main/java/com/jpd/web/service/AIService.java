package com.jpd.web.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.exception.AIHandlerException;
import com.jpd.web.model.WritingResult;
import com.jpd.web.repository.WritingResultRepository;
import com.jpd.web.dto.WritingScores;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AIService {

    private final WritingResultRepository writingResultRepository;
    private final GeminiAiService geminiAiService;
    private final FireBaseService fireBaseService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AIService(
            WritingResultRepository writingResultRepository,
            GeminiAiService geminiAiService,
            FireBaseService fireBaseService,
            ObjectMapper objectMapper
    ) {
        this.writingResultRepository = writingResultRepository;
        this.geminiAiService = geminiAiService;
        this.fireBaseService = fireBaseService;
        this.objectMapper = objectMapper;
    }

    // ----- Public API -----

    public String generateFeedback(String question, String answer) {
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

        return geminiAiService.generateContent(sb.toString());
    }

    /**
     * Evaluate writing and persist a WritingResult (customerId may be null).
     * Returns a Map normalized for the frontend: keys: grammar, vocabulary, feedback.
     */
    public Map<String, Object> evaluateWriting(String writingText, String language, String customerId) {
        String prompt = buildEvaluationPrompt(writingText, language);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // 00:00:00
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        if(this.writingResultRepository.countTodayByCustomerId(customerId,startOfDay,endOfDay) >= 10) {
            throw new AIHandlerException("You can only evaluate 10 times per day");
        }
        String aiResponse = geminiAiService.generateContent(prompt);

        log.info("Raw AI Response: {}", aiResponse);

        try {
            String cleanJson = cleanJsonResponse(aiResponse);
            log.info("Cleaned JSON: {}", cleanJson);

            Map<String, Object> result = objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});

            double grammar = getScore(result, "grammar", "grammar_score");
            double vocabulary = getScore(result, "vocabulary", "vocabulary_score");
            String feedback = (String) result.getOrDefault("feedback", "No feedback provided");

            WritingResult entity = WritingResult.builder()
                    .customerId(customerId)
                    .grammar(grammar)
                    .vocabulary(vocabulary)
                    .feedback(feedback)
                    .build();

            writingResultRepository.save(entity);

            Map<String, Object> normalized = new HashMap<>();
            normalized.put("grammar", grammar);
            normalized.put("vocabulary", vocabulary);
            normalized.put("feedback", feedback);

            return normalized;
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", aiResponse, e);
            return Map.of(
                    "grammar", 0,
                    "vocabulary", 0,
                    "feedback", "Error evaluating writing. Please try again."
            );
        }
    }

    /**
     * Simple version returning a WritingScores DTO.
     * When no customerId is available, pass null to persist (if desired).
     */
    public WritingScores evaluateWritingSimple(String writingText, String language,String customerId) {
        try {
            Map<String, Object> result = evaluateWriting(writingText, language, customerId);

            return new WritingScores(
                    ((Number) result.get("grammar")).doubleValue(),
                    ((Number) result.get("vocabulary")).doubleValue(),
                    (String) result.get("feedback")
            );
        } catch (Exception e) {
            log.error("Error evaluating writing", e);
            return new WritingScores(
                    5.0,
                    5.0,
                    "Could not evaluate. Please try again."
            );
        }
    }

    // ----- Helpers -----

    private String escapeForPrompt(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }

    /**
     * Clean a possible markdown-wrapped JSON response from the AI.
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "{}";
        }

        String cleaned = response.trim()
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private String buildEvaluationPrompt(String writingText, String language) {
        return String.format("""
            You are an expert %s language teacher. Evaluate this writing:
            
            '''
            %s
            '''
            
            Return ONLY valid JSON (no markdown, no extra text):
            {
                "grammar": <score 0-10>,
                "vocabulary": <score 0-10>,
                "feedback": "<detailed constructive feedback in Vietnamese>"
            }
            
            Be specific about grammar errors and vocabulary usage.
            """, language, writingText);
    }

    private double getScore(Map<String, Object> result, String... keys) {
        for (String key : keys) {
            Object value = result.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        return 0.0;
    }
}
