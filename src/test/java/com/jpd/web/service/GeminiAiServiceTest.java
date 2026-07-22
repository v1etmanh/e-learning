package com.jpd.web.service;

import com.jpd.web.exception.UsageLimitExceededException;
import com.jpd.web.service.utils.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiAiServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private FeedbackUsageService feedbackUsageService;

    private GeminiAiService geminiAiService;

    @BeforeEach
    void setUp() {
        geminiAiService = new GeminiAiService();
        ReflectionTestUtils.setField(geminiAiService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(geminiAiService, "feedbackUsageService", feedbackUsageService);
        ReflectionTestUtils.setField(geminiAiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiAiService, "apiUrl", "https://gemini.example.com");
    }

    private Map<String, Object> geminiResponseBody(String text) {
        return Map.of("candidates", List.of(
                Map.of("content", Map.of("parts", List.of(Map.of("text", text))))));
    }

    @Nested
    class GenerateContent {

        @Test
        void parsesTextFromFirstCandidate() {
            when(restTemplate.exchange(eq("https://gemini.example.com"), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(geminiResponseBody("Hello from Gemini")));

            String result = geminiAiService.generateContent("prompt");

            assertThat(result).isEqualTo("Hello from Gemini");
        }

        @Test
        void fallsBackToDefaultMessage_whenCandidatesEmpty() {
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("candidates", List.of())));

            String result = geminiAiService.generateContent("prompt");

            assertThat(result).isEqualTo("No response from Gemini");
        }

        @Test
        void fallsBackToDefaultMessage_whenCandidatesKeyMissing() {
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("error", "something")));

            String result = geminiAiService.generateContent("prompt");

            assertThat(result).isEqualTo("No response from Gemini");
        }
    }

    @Nested
    class GenerateFeedback {

        @Test
        void checksUsageBeforeCallingGemini() {
            when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(geminiResponseBody("feedback text")));

            geminiAiService.generateFeedback("user-1", SubscriptionTier.FREE, "question?", "answer");

            verify(feedbackUsageService).checkAndRecordUsage("user-1", SubscriptionTier.FREE);
            verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class));
        }

        @Test
        void usageLimitExceeded_shortCircuits_neverCallsRestTemplate() {
            org.mockito.Mockito.doThrow(new UsageLimitExceededException("DAILY", 5, 5))
                    .when(feedbackUsageService).checkAndRecordUsage("user-1", SubscriptionTier.FREE);

            assertThrows(UsageLimitExceededException.class,
                    () -> geminiAiService.generateFeedback("user-1", SubscriptionTier.FREE, "q", "a"));

            verifyNoInteractions(restTemplate);
        }
    }

    @Test
    void generateContentWithImage_appendsInstructionAndIncludesInlineData() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiResponseBody("[\"a\",\"b\"]")));

        String result = geminiAiService.generateContentWithImage("Describe this", "base64data", "image/png");

        assertThat(result).isEqualTo("[\"a\",\"b\"]");
    }
}
