package com.jpd.web.integration.controller.creator;

import com.google.firebase.cloud.StorageClient;
import com.jpd.web.testutil.JwtTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates an authenticated /api/** path (no specific role required) and the
 * MethodArgumentNotValidException -> 400 VALIDATION_ERROR mapping for an invalid
 * multipart CourseFormDto submission.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseControllerIntegrationTest {

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
        mockMvc.perform(get("/api/creator/course"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequest_getsPastSecurity_reachesController() throws Exception {
        // No course-owning creator exists in the (empty) test DB for this subject,
        // so CourseService.retrieveCourseByemail's unguarded Optional.get() throws
        // NoSuchElementException, surfacing as 500 - but critically NOT 401/403,
        // proving the request cleared the authentication gate.
        mockMvc.perform(get("/api/creator/course")
                        .with(JwtTestDataBuilder.jwtForCreator("creator-1")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createCourse_withMissingRequiredFields_returns400ValidationError() throws Exception {
        mockMvc.perform(multipart("/api/creator/course/create")
                        .with(JwtTestDataBuilder.jwtForCreator("creator-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
