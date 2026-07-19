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
import com.jpd.web.dto.MagicDiaryScores;
import com.jpd.web.dto.MagicDiaryQuestionResponse;
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

    public String evaluateIeltsBrainstorm(String prompt) {
        return geminiAiService.generateContent(prompt);
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

    public MagicDiaryScores evaluateMagicDiary(String writingText, String language, String lessonTitle,
                                                String referenceNotes, String customerId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        if (this.writingResultRepository.countTodayByCustomerId(customerId, startOfDay, endOfDay) >= 10) {
            throw new AIHandlerException("You can only evaluate 10 times per day");
        }

        String prompt = buildMagicDiaryPrompt(writingText, language, lessonTitle, referenceNotes);
        try {
            String cleanJson = cleanJsonResponse(geminiAiService.generateContent(prompt));
            Map<String, Object> result = objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
            double contentAccuracy = getScore(result, "contentAccuracy", "content_accuracy", "history");
            double grammar = getScore(result, "grammar", "grammar_score");
            double vocabulary = getScore(result, "vocabulary", "vocabulary_score");
            String feedback = String.valueOf(result.getOrDefault("feedback", "No feedback provided"));
            String nextStep = String.valueOf(result.getOrDefault("nextStep", "Review the lesson and write one more paragraph."));

            writingResultRepository.save(WritingResult.builder()
                    .customerId(customerId)
                    .grammar(grammar)
                    .vocabulary(vocabulary)
                    .feedback(feedback)
                    .build());

            return MagicDiaryScores.builder()
                    .contentAccuracy(contentAccuracy)
                    .grammar(grammar)
                    .vocabulary(vocabulary)
                    .feedback(feedback)
                    .nextStep(nextStep)
                    .build();
        } catch (Exception exception) {
            log.error("Error evaluating magic diary writing", exception);
            return MagicDiaryScores.builder()
                    .contentAccuracy(5.0)
                    .grammar(5.0)
                    .vocabulary(5.0)
                    .feedback("Could not evaluate. Please try again.")
                    .nextStep("Review the key facts and rewrite your answer with one clearer detail.")
                    .build();
        }
    }

    public MagicDiaryQuestionResponse answerMagicDiaryQuestion(String question, String lessonTitle,
                                                                 String referenceNotes, String customerId) {
        String prompt = buildMagicDiaryQuestionPrompt(question, lessonTitle, referenceNotes);
        try {
            String cleanJson = cleanJsonResponse(geminiAiService.generateContent(prompt));
            Map<String, Object> result = objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
            String status = String.valueOf(result.getOrDefault("status", "OUT_OF_SCOPE"));
            String answer = String.valueOf(result.getOrDefault("answer", ""));
            String correction = String.valueOf(result.getOrDefault("correction", ""));
            String suggestedQuestion = String.valueOf(result.getOrDefault("suggestedQuestion", ""));
            return MagicDiaryQuestionResponse.builder()
                    .status(status)
                    .answer(answer)
                    .correction(correction)
                    .suggestedQuestion(suggestedQuestion)
                    .build();
        } catch (Exception exception) {
            log.error("Error answering magic diary question", exception);
            return MagicDiaryQuestionResponse.builder()
                    .status("ERROR")
                    .answer("")
                    .correction("Cuốn sách chưa thể đọc câu hỏi này. Hãy viết lại ngắn và rõ hơn.")
                    .suggestedQuestion("What was one important change in the lesson?")
                    .build();
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

    private String buildMagicDiaryPrompt(String writingText, String language, String lessonTitle,
                                         String referenceNotes) {
        return String.format("""
                You are an expert %s history and English writing tutor.
                Evaluate the learner's answer for the magic diary lesson: %s.
                Compare the answer only with the verified lesson notes below; do not require exact wording.

                VERIFIED LESSON NOTES:
                %s

                LEARNER'S WRITING:
                %s

                Return ONLY valid JSON with no markdown:
                {
                  "contentAccuracy": <score 0-10 based on historical facts and key ideas>,
                  "grammar": <score 0-10 based on English grammar and sentence structure>,
                  "vocabulary": <score 0-10 based on useful and accurate word choice>,
                  "feedback": "short constructive feedback in Vietnamese",
                  "nextStep": "one focused next practice task in Vietnamese"
                }
                Keep contentAccuracy separate from grammar and vocabulary.
                """, language, escapeForPrompt(lessonTitle), escapeForPrompt(referenceNotes), escapeForPrompt(writingText));
    }

    private String buildMagicDiaryQuestionPrompt(String question, String lessonTitle, String referenceNotes) {
        return String.format("""
                You are the living hint inside a magical diary for the lesson: %s.
                You are a strict retrieval-grounded tutor. You may use ONLY the verified lesson context below.
                Never use outside facts, guesses, general knowledge, or information not supported by the context.

                VERIFIED LESSON CONTEXT:
                %s

                LEARNER QUESTION:
                %s

                Follow this order exactly:
                1. Check spelling and vocabulary/word choice in the learner question.
                2. Estimate the ratio of incorrect or unintelligible words to the total words.
                3. If the error ratio is greater than 20%%, do not answer the question. Return status CORRECTION_REQUIRED, give a concise corrected version in correction, leave answer empty, and provide a suggestedQuestion.
                4. If the error ratio is 20%% or less, correct minor errors if needed and continue.
                5. If the question is not answerable from the verified lesson context, return status OUT_OF_SCOPE, leave answer empty, and explain that the diary only answers questions about this lesson.
                6. Otherwise return status ANSWERED and answer in clear, short English (2-4 sentences) using only the verified context.

                Return ONLY valid JSON, with no markdown:
                {
                  "status": "ANSWERED|CORRECTION_REQUIRED|OUT_OF_SCOPE",
                  "answer": "",
                  "correction": "",
                  "suggestedQuestion": ""
                }
                """, escapeForPrompt(lessonTitle), escapeForPrompt(referenceNotes), escapeForPrompt(question));
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
