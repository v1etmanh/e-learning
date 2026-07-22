package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.jpd.web.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CertificateApprovalDto", description = "A creator application awaiting certificate review.")
public class CertificateApprovalDto {
    @Schema(description = "Creator identifier - the Keycloak user id.", example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
    private String creatorId;
    @Schema(description = "Applicant name.", example = "Akiko Suzuki")
    private String fullName;
    @Schema(description = "Uploaded qualification documents for the admin to review.")
    private List<String> certificateUrls;
    @Schema(description = "When the application was submitted.", example = "2026-07-20")
    private Date submittedAt;
    @Schema(description = "Review state. Always `PENDING` in this listing.", example = "PENDING")
    private Status status;
    @Schema(description = "Note left by the reviewing admin.", example = "null")
    private String adminNote;
}
