package com.jpd.web.integration.controller.common;

import com.google.firebase.cloud.StorageClient;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The one test class in this suite that deliberately never attaches a jwt() post-processor -
 * proves the explicit permitAll() carve-out for /api/course/* in JaenConfig actually works
 * for a genuinely anonymous request through the real filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseInfControllerIntegrationTest {

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
    void anonymousRequest_toRecommendCourses_succeeds() throws Exception {
        mockMvc.perform(get("/api/course/recommend_courses"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousRequest_toSearch_succeeds() throws Exception {
        mockMvc.perform(get("/api/course/search").param("name", "java"))
                .andExpect(status().isOk());
    }
}
