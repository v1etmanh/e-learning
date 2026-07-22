package com.jpd.web.integration.controller.admin;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates the {@code hasRole("ADMIN")} rule on /api/admin/** end-to-end through the
 * real Spring Security filter chain, plus confirms an uncaught exception in the service
 * layer (AdminCreatorService.getCreatorDetail NPEs on a missing creator - see AdminCreatorServiceTest)
 * surfaces as a generic 500 via GlobalExceptionHandler's catch-all, not a 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCreatorControllerIntegrationTest {

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
        mockMvc.perform(get("/api/admin/creators/nonexistent-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminRole_isRejected_withForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/creators/nonexistent-id")
                        .with(JwtTestDataBuilder.jwtForCreator("creator-1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_reachesController_missingCreatorSurfacesAsGenericServerError() throws Exception {
        // Confirms the ADMIN role gets past the security filter chain (not 401/403), and
        // that AdminCreatorService's unguarded NPE on a missing creator id falls through
        // GlobalExceptionHandler's generic Exception handler as a 500, not a 404 -
        // pinning the real end-to-end behavior of a known service-layer gap.
        mockMvc.perform(get("/api/admin/creators/nonexistent-id")
                        .with(JwtTestDataBuilder.jwtForAdmin("admin-1")))
                .andExpect(status().isInternalServerError());
    }
}
