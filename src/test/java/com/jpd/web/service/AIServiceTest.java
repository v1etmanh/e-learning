package com.jpd.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.dto.MagicDiaryQuestionResponse;
import com.jpd.web.dto.MagicDiaryScores;
import com.jpd.web.dto.WritingScores;
import com.jpd.web.exception.AIHandlerException;
import com.jpd.web.model.WritingResult;
import com.jpd.web.repository.WritingResultRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private WritingResultRepository writingResultRepository;
    @Mock
    private GeminiAiService geminiAiService;
    @Mock
    private FireBaseService fireBaseService;

    private AIService aiService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        aiService = new AIService(writingResultRepository, geminiAiService, fireBaseService, new ObjectMapper());
    }

    @Test
    void generateFeedback_promptContainsEscapedQuestionAndAnswer() {
        when(geminiAiService.generateContent(anyString())).thenReturn("ok");

        aiService.generateFeedback("What is \"this\"?", "line1\nline2");

        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(geminiAiService).generateContent(captor.capture());
        assertThat(captor.getValue()).contains("What is \\\"this\\\"?");
        assertThat(captor.getValue()).contains("line1 line2");
    }

    @Nested
    class EvaluateWriting {

        @Test
        void throwsAIHandlerException_whenDailyLimitReached() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(10L);

            assertThrows(AIHandlerException.class,
                    () -> aiService.evaluateWriting("text", "en", "customer-1"));
        }

        @Test
        void parsesFencedJsonResponse() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn(
                    "```json\n{\"grammar\":8,\"vocabulary\":7,\"feedback\":\"good\"}\n```");

            java.util.Map<String, Object> result = aiService.evaluateWriting("text", "en", "customer-1");

            assertThat(result.get("grammar")).isEqualTo(8.0);
            assertThat(result.get("vocabulary")).isEqualTo(7.0);
            assertThat(result.get("feedback")).isEqualTo("good");
        }

        @Test
        void parsesUnfencedJsonWithSurroundingText() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn(
                    "Here is the result: {\"grammar\":5,\"vocabulary\":6,\"feedback\":\"ok\"} thanks");

            java.util.Map<String, Object> result = aiService.evaluateWriting("text", "en", "customer-1");

            assertThat(result.get("grammar")).isEqualTo(5.0);
        }

        @Test
        void fallsBackToErrorMap_whenResponseHasNoBraces() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn("not json at all");

            java.util.Map<String, Object> result = aiService.evaluateWriting("text", "en", "customer-1");

            assertThat(result.get("grammar")).isEqualTo(0);
            assertThat(result.get("feedback")).isEqualTo("Error evaluating writing. Please try again.");
            verify(writingResultRepository, never()).save(any());
        }

        @Test
        void multiKeyAliasScoreParsing_prefersPrimaryKeyOverScoreSuffixedKey() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn(
                    "{\"grammar\":9,\"grammar_score\":1,\"vocabulary_score\":4,\"feedback\":\"x\"}");

            java.util.Map<String, Object> result = aiService.evaluateWriting("text", "en", "customer-1");

            assertThat(result.get("grammar")).isEqualTo(9.0);
            assertThat(result.get("vocabulary")).isEqualTo(4.0);
        }

        @Test
        void savesWritingResult_onSuccessfulParse() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn(
                    "{\"grammar\":8,\"vocabulary\":7,\"feedback\":\"good\"}");

            aiService.evaluateWriting("text", "en", "customer-1");

            ArgumentCaptor<WritingResult> captor = ArgumentCaptor.forClass(WritingResult.class);
            verify(writingResultRepository).save(captor.capture());
            assertThat(captor.getValue().getCustomerId()).isEqualTo("customer-1");
        }
    }

    @Nested
    class EvaluateWritingSimple {

        @Test
        void swallowsAnyException_includingDailyLimit_returnsFixedFallback() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(10L);

            WritingScores result = aiService.evaluateWritingSimple("text", "en", "customer-1");

            assertThat(result.getGrammar()).isEqualTo(5.0);
            assertThat(result.getVocabulary()).isEqualTo(5.0);
        }

        @Test
        void returnsRealScores_onSuccess() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn(
                    "{\"grammar\":8,\"vocabulary\":7,\"feedback\":\"good\"}");

            WritingScores result = aiService.evaluateWritingSimple("text", "en", "customer-1");

            assertThat(result.getGrammar()).isEqualTo(8.0);
        }
    }

    @Nested
    class EvaluateMagicDiary {

        @Test
        void throwsAIHandlerException_whenDailyLimitReached() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(10L);

            assertThrows(AIHandlerException.class,
                    () -> aiService.evaluateMagicDiary("text", "en", "lesson", "notes", "customer-1"));
        }

        @Test
        void contentAccuracyAliasChain_fallsBackToHistoryKey() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn(
                    "{\"history\":9,\"grammar\":8,\"vocabulary\":7,\"feedback\":\"good\"}");

            MagicDiaryScores result = aiService.evaluateMagicDiary("text", "en", "lesson", "notes", "customer-1");

            assertThat(result.getContentAccuracy()).isEqualTo(9.0);
        }

        @Test
        void parseFailure_returnsDistinctFallbackContent() {
            when(writingResultRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            when(geminiAiService.generateContent(anyString())).thenReturn("garbage");

            MagicDiaryScores result = aiService.evaluateMagicDiary("text", "en", "lesson", "notes", "customer-1");

            assertThat(result.getContentAccuracy()).isEqualTo(5.0);
            assertThat(result.getFeedback()).isEqualTo("Could not evaluate. Please try again.");
        }
    }

    @Nested
    class AnswerMagicDiaryQuestion {

        @Test
        void defaultsToOutOfScope_whenKeysAbsent() {
            when(geminiAiService.generateContent(anyString())).thenReturn("{}");

            MagicDiaryQuestionResponse result =
                    aiService.answerMagicDiaryQuestion("question", "lesson", "notes", "customer-1");

            assertThat(result.getStatus()).isEqualTo("OUT_OF_SCOPE");
            assertThat(result.getAnswer()).isEmpty();
        }

        @Test
        void errorPath_returnsErrorStatus() {
            when(geminiAiService.generateContent(anyString())).thenThrow(new RuntimeException("gemini down"));

            MagicDiaryQuestionResponse result =
                    aiService.answerMagicDiaryQuestion("question", "lesson", "notes", "customer-1");

            assertThat(result.getStatus()).isEqualTo("ERROR");
        }

        @Test
        void neverChecksDailyLimit_regardlessOfRepositoryState() {
            when(geminiAiService.generateContent(anyString())).thenReturn("{}");

            aiService.answerMagicDiaryQuestion("question", "lesson", "notes", "customer-1");

            verifyNoInteractions(writingResultRepository);
        }
    }
}
