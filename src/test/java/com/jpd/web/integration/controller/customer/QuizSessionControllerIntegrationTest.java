package com.jpd.web.integration.controller.customer;

import com.google.firebase.cloud.StorageClient;
import com.jpd.web.testutil.JwtTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QuizSessionController.submitAnswer has no try/catch of its own - any exception from
 * SessionService.submitAnswer propagates straight to the dispatcher. SessionService wraps
 * a missing session as a plain RuntimeException (not one of GlobalExceptionHandler's mapped
 * BusinessException subtypes), so this confirms that gap surfaces as a generic 500 end-to-end,
 * not something inferred only from reading the handler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuizSessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageClient storageClient;
    @MockBean
    private S3Client s3Client;
    @MockBean
    private S3Presigner s3Presigner;
    @MockBean
    private JavaMailSender javaMailSender;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void submitAnswer_forNonexistentSession_surfacesAsGenericServerError() throws Exception {
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("quiz:session:MISSING")).thenReturn(null);

        String body = "{\"sessionCode\":\"MISSING\",\"participantId\":\"p1\",\"questionId\":1,\"answer\":\"A\"}";

        mockMvc.perform(post("/api/quiz/submit-answer")
                        .with(JwtTestDataBuilder.jwtForCustomer("customer-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
