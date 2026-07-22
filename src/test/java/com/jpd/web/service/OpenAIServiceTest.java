package com.jpd.web.service;

import com.jpd.web.model.SemanticResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private MultipartFile multipartFile;

    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        openAIService = new OpenAIService();
        ReflectionTestUtils.setField(openAIService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(openAIService, "openaiApiKey", "test-key");
    }

    @Test
    void calculateCosineSimilarity_orthogonalVectors_isZero() {
        double result = openAIService.calculateCosineSimilarity(new double[]{1, 0}, new double[]{0, 1});
        assertThat(result).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void calculateCosineSimilarity_identicalVectors_isOne() {
        double result = openAIService.calculateCosineSimilarity(new double[]{1, 2, 3}, new double[]{1, 2, 3});
        assertThat(result).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void calculateCosineSimilarity_knownNonTrivialPair() {
        // cos angle between [1,0] and [1,1] = 1/sqrt(2)
        double result = openAIService.calculateCosineSimilarity(new double[]{1, 0}, new double[]{1, 1});
        assertThat(result).isCloseTo(1.0 / Math.sqrt(2), within(1e-9));
    }

    @Nested
    class GenerateSemanticFeedback {

        private String feedbackFor(double similarity) {
            return ReflectionTestUtils.invokeMethod(openAIService, "generateSemanticFeedback", similarity);
        }

        @Test
        void fiveDistinctBands_eachWithDistinctMessage() {
            assertThat(feedbackFor(0.95)).contains("Xuất sắc");
            assertThat(feedbackFor(0.99)).contains("Xuất sắc");
            assertThat(feedbackFor(0.85)).contains("Rất tốt");
            assertThat(feedbackFor(0.90)).contains("Rất tốt");
            assertThat(feedbackFor(0.70)).contains("Khá tốt");
            assertThat(feedbackFor(0.80)).contains("Khá tốt");
            assertThat(feedbackFor(0.50)).contains("Cần cải thiện");
            assertThat(feedbackFor(0.60)).contains("Cần cải thiện");
            assertThat(feedbackFor(0.49)).contains("Hãy thử lại");
            assertThat(feedbackFor(0.0)).contains("Hãy thử lại");
        }
    }

    @Nested
    class CompareSemanticWithEmbedding {

        private void mockEmbeddingResponse(double[] embedding) {
            String json = "{\"data\":[{\"embedding\":" + java.util.Arrays.toString(embedding) + "}]}";
            when(restTemplate.exchange(eq("https://api.openai.com/v1/embeddings"), eq(HttpMethod.POST), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(json));
        }

        @Test
        void isCorrectTrue_whenSimilarityAtOrAboveEightyPercent() throws Exception {
            mockEmbeddingResponse(new double[]{1, 2, 3});

            SemanticResult result = openAIService.compareSemanticWithEmbedding("same text", "same text");

            assertThat(result.isMatch()).isTrue();
            assertThat(result.getSimilarityScore()).isCloseTo(1.0, within(1e-9));
        }
    }

    @Nested
    class SpeechToText {

        @Test
        void wrapsIOException_fromAudioBytes_intoGenericException() throws IOException {
            when(multipartFile.getBytes()).thenThrow(new IOException("disk error"));

            Exception ex = assertThrows(Exception.class, () -> openAIService.speechToText(multipartFile, "en"));
            assertThat(ex.getMessage()).isEqualTo("Không thể xử lý file audio");
        }

        @Test
        void parsesTranscribedText_fromResponse() throws Exception {
            MockMultipartFile audio = new MockMultipartFile("audio", "a.wav", "audio/wav", new byte[]{1, 2, 3});
            when(restTemplate.exchange(eq("https://api.openai.com/v1/audio/transcriptions"), eq(HttpMethod.POST), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("{\"text\":\"hello world\"}"));

            String result = openAIService.speechToText(audio, "en");

            assertThat(result).isEqualTo("hello world");
        }
    }
}
