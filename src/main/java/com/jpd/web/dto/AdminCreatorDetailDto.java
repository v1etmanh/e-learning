package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.jpd.web.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AdminCreatorDetailDto", description = "Full creator record for moderation: profile, statistics, ban state and recent activity.")
public class AdminCreatorDetailDto {
    // Basic info
    @Schema(description = "Creator identifier - the Keycloak user id.", example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
    private String creatorId;
    @Schema(description = "Creator name.", example = "Akiko Suzuki")
    private String fullName;

    @Schema(description = "Contact phone number.", example = "0912345678")
    private String phone;
    @Schema(description = "Creator self-description.", example = "Giao vien tieng Nhat 5 nam kinh nghiem.")
    private String titleSelf;
    @Schema(description = "Avatar image URL.", example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Favatar.jpg?alt=media")
    private String imageUrl;

    // Status & verification
    @Schema(description = "Review state of the creator profile.", example = "SUCCESS")
    private Status status;
    @Schema(description = "Uploaded qualification documents.")
    private List<String> certificateUrls;
    @Schema(description = "PayPal account used for payouts.", example = "akiko.suzuki@example.com")
    private String paymentEmail;

    // Statistics
    @Schema(description = "Unpaid earnings balance in VND.", example = "15750000.0")
    private Double balance;
    @Schema(description = "Lifetime revenue in VND.", example = "82400000.0")
    private Double totalRevenue;
    @Schema(description = "Number of courses published.", example = "12")
    private Integer totalCourses;
    @Schema(description = "Students across all their courses.", example = "2847")
    private Integer totalStudents;
    @Schema(description = "Average rating across all their courses.", example = "4.8", minimum = "0", maximum = "5")
    private Double avgRating;

    // Moderation
    @Schema(description = "Warnings issued by admins. Three within 90 days auto-suspends the creator.", example = "1")
    private Integer warningCount;
    @Schema(description = "Internal reputation score used to prioritise moderation.", example = "82")
    private Integer reputationScore;
    @Schema(description = "Whether the creator is currently banned.", example = "false")
    private Boolean isBanned;
    @Schema(description = "End of a temporary ban. Null for a permanent ban or when not banned.", example = "null")
    private Date bannedUntil;

    // Recent activity
    @Schema(description = "The ten most recent reports filed against this creator.")
    private List<ReportSummaryDto> recentReports; // last 10
    @Schema(description = "The creator's five most recent courses.")
    private List<CourseCardDto> recentCourses; // top 5

    // Audit
    @Schema(description = "When the creator profile was created.", example = "2025-11-03")
    private Date createDate;
    @Schema(description = "When the creator last did anything on the platform.", example = "2026-07-21")
    private Date lastActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ReportSummaryDto", description = "Condensed report entry shown on a creator's moderation record.")
    public static class ReportSummaryDto {
        @Schema(description = "Report identifier.", example = "551")
        private Long reportId;
        @Schema(description = "Reason the course was reported. One of the `ReportType` values.", example = "MISLEADING_INFORMATION")
        private String reportType;
        @Schema(description = "What the reporter wrote.", example = "Noi dung khong dung mo ta.")
        private String detail;
        @Schema(description = "Where the report is in the review process.", example = "PENDING")
        private String status;
        @Schema(description = "When the report was filed.", example = "2026-07-18")
        private LocalDate createdAt;
        @Schema(description = "When an admin reviewed it. Null while still pending.", example = "null")
        private LocalDate reviewedAt;
    }
}
