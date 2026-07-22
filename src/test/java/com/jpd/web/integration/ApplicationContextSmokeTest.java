package com.jpd.web.integration;

import com.google.firebase.cloud.StorageClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Verifies the full Spring context boots against H2 with every external SDK
 * client mocked out. This is the chunk-0 sanity check every later
 * {@code @SpringBootTest} in this project relies on staying green.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextSmokeTest {

    @MockBean
    private StorageClient storageClient;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private S3Presigner s3Presigner;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
    }
}
