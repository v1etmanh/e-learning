package com.jpd.web.integration.controller.customer;

import com.google.firebase.cloud.StorageClient;
import com.jpd.web.testutil.JwtTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates a plain .authenticated() /api/** path (no specific role) and the
 * CourseNotFoundException -> 404 mapping live through the real dispatcher.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrollmentControllerIntegrationTest {

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

    @Test
    void anonymousRequest_isRejected_withUnauthorized() throws Exception {
        mockMvc.perform(post("/api/enroll/1").param("joinKey", "ABC123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequest_onNonexistentCourse_returns404CourseNotFound() throws Exception {
        mockMvc.perform(post("/api/enroll/1")
                        .param("joinKey", "ABC123")
                        .with(JwtTestDataBuilder.jwtForCustomer("customer-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }
}
