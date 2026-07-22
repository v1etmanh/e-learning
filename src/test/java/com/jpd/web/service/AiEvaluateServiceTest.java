package com.jpd.web.service;

import com.jpd.web.exception.AIHandlerException;
import com.jpd.web.model.SemanticResult;
import com.jpd.web.repository.SemanticDataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiEvaluateServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);

    @Mock
    private OpenAIService openAIService;
    @Mock
    private SemanticDataRepository semanticDataRepository;

    @InjectMocks
    private AiEvaluateService aiEvaluateService;

    private MockedStatic<LocalDate> mockedLocalDate;

    @BeforeEach
    void pinToday() {
        mockedLocalDate = mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        mockedLocalDate.when(LocalDate::now).thenReturn(TODAY);
    }

    @AfterEach
    void unpinToday() {
        mockedLocalDate.close();
    }

    private MockMultipartFile audioFile() {
        return new MockMultipartFile("audio", "audio.wav", "audio/wav", new byte[]{1, 2, 3});
    }

    @Test
    void throwsAIHandlerException_whenDailyLimitExceeded_boundaryIsGreaterThanTen() throws Exception {
        when(semanticDataRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(11L);

        assertThrows(AIHandlerException.class,
                () -> aiEvaluateService.evaluateSpeaking(audioFile(), "expected", "en", "customer-1", 1L));

        verify(openAIService, never()).speechToText(any(), any());
    }

    @Test
    void allowsEvaluation_whenCountIsExactlyTen() throws Exception {
        when(semanticDataRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(10L);
        when(openAIService.speechToText(any(), any())).thenReturn("transcribed text");
        SemanticResult result = SemanticResult.builder().match(true).similarityScore(0.9)
                .userAnswer("transcribed text").expectedAnswer("expected").build();
        when(openAIService.compareSemanticWithEmbedding("transcribed text", "expected")).thenReturn(result);

        SemanticResult actual = aiEvaluateService.evaluateSpeaking(audioFile(), "expected", "en", "customer-1", 1L);

        assertThat(actual).isSameAs(result);
        verify(semanticDataRepository).save(any());
    }

    @Nested
    class ValidateInputs {

        @Test
        void throwsAIHandlerException_whenAudioEmpty() {
            when(semanticDataRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);
            MockMultipartFile empty = new MockMultipartFile("audio", new byte[0]);

            assertThrows(AIHandlerException.class,
                    () -> aiEvaluateService.evaluateSpeaking(empty, "expected", "en", "customer-1", 1L));
        }

        @Test
        void throwsAIHandlerException_whenExpectedAnswerBlank() {
            when(semanticDataRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);

            assertThrows(AIHandlerException.class,
                    () -> aiEvaluateService.evaluateSpeaking(audioFile(), "  ", "en", "customer-1", 1L));
        }

        @Test
        void throwsAIHandlerException_whenLanguageNull() {
            when(semanticDataRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(0L);

            assertThrows(AIHandlerException.class,
                    () -> aiEvaluateService.evaluateSpeaking(audioFile(), "expected", null, "customer-1", 1L));
        }

        @Test
        void doesNotCallOpenAI_whenDailyLimitAlreadyExceeded_evenWithValidInputs() throws Exception {
            when(semanticDataRepository.countTodayByCustomerId(anyString(), any(), any())).thenReturn(999L);

            assertThrows(AIHandlerException.class,
                    () -> aiEvaluateService.evaluateSpeaking(audioFile(), "expected", "en", "customer-1", 1L));

            verify(openAIService, never()).speechToText(any(), any());
            verify(openAIService, never()).compareSemanticWithEmbedding(any(), any());
        }
    }
}
