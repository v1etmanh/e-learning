package com.jpd.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive Admin Dashboard Overview Response DTO
 * Contains all metrics and statistics for the admin dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardOverviewResponse {

    private UserStats userStats;
    private CourseStats courseStats;
    private EnrollmentStats enrollmentStats;
    private RevenueStats revenueStats;
    private ModerationStats moderationStats;
    private EngagementStats engagementStats;
    private SystemHealthStats systemHealthStats;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;
    
    private String period; // e.g., "Last 30 days", "Custom: 2024-01-01 to 2024-12-31"

    /**
     * User-related statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long totalUsers;
        private Long totalCreators;
        private Long totalCustomers;
        private Long activeUsersLast30Days;
        private Long newUsersToday;
        private Long newUsersThisWeek;
        private Long newUsersThisMonth;
        private Double userGrowthRate; // Percentage
        private Long bannedCreators;
        private Long creatorsPendingApproval;
        private List<DailyUserTrend> dailyTrends;
    }

    /**
     * Course-related statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseStats {
        private Long totalCourses;
        private Long publishedCourses;
        private Long privateCourses;
        private Long bannedCourses;
        private Long pendingApproval;
        private Long coursesCreatedToday;
        private Long coursesCreatedThisWeek;
        private Long coursesCreatedThisMonth;
        private Double averageCoursesPerCreator;
        private Double courseGrowthRate; // Percentage
        private List<CourseByLanguage> coursesByLanguage;
        private List<CourseByAccessMode> coursesByAccessMode;
    }

    /**
     * Enrollment-related statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentStats {
        private Long totalEnrollments;
        private Long activeEnrollments;
        private Long completedEnrollments;
        private Double completionRate; // Percentage
        private Long enrollmentsToday;
        private Long enrollmentsThisWeek;
        private Long enrollmentsThisMonth;
        private Double enrollmentGrowthRate; // Percentage
        private Double averageEnrollmentsPerCourse;
        private Long totalCourseCompletions;
        private List<DailyEnrollmentTrend> dailyTrends;
    }

    /**
     * Revenue and financial statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        private Double totalCreatorBalance;
        private Double averageCreatorBalance;
        private Double highestCreatorBalance;
        private Long creatorsWithBalance;
        private Long creatorsWithZeroBalance;
        private Double totalBalanceThisMonth;
        private Double totalBalanceLastMonth;
        private Double revenueGrowthRate; // Percentage
        private List<TopRevenueCreator> topRevenueCreators;
    }

    /**
     * Content moderation statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModerationStats {
        private Long totalReports;
        private Long newReports;
        private Long reviewingReports;
        private Long resolvedReports;
        private Long dismissedReports;
        private Long pendingReportReview;
        private Long totalWarnings;
        private Long activeWarnings;
        private Long bannedCreators;
        private Long bannedCourses;
        private Double averageReportsPerCourse;
        private List<ReportByType> reportsByType;
        private List<RecentReport> recentReports;
    }

    /**
     * Engagement metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementStats {
        private Long totalComments;
        private Long commentsToday;
        private Long commentsThisWeek;
        private Long commentsThisMonth;
        private Long totalFeedbacks;
        private Long feedbacksThisMonth;
        private Double averageRating;
        private Long totalRatings;
        private Long totalWishlists;
        private Long wishlistsThisMonth;
        private Double averageCommentsPerCourse;
        private Double averageFeedbacksPerCourse;
        private List<RatingDistribution> ratingDistribution;
    }

    /**
     * System health and performance metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealthStats {
        private Long totalDatabaseRecords;
        private Long totalCourseMetrics;
        private Long orphanedEnrollments; // Enrollments without valid course
        private Long coursesWithoutMetrics;
        private Long creatorsWithoutCourses;
        private Double databaseHealthScore; // 0-100
        private String status; // HEALTHY, WARNING, CRITICAL
        private LocalDateTime lastDataSync;
    }

    // Supporting nested classes for detailed breakdowns

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUserTrend {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Long newUsers;
        private Long activeUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyEnrollmentTrend {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Long enrollments;
        private Long completions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseByLanguage {
        private String language;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseByAccessMode {
        private String accessMode;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRevenueCreator {
        private String creatorId;
        private String fullName;
        private Double balance;
        private Long totalCourses;
        private Long totalStudents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByType {
        private String reportType;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReport {
        private Long reportId;
        private String reportType;
        private String courseName;
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private Integer rating; // 1-5
        private Long count;
        private Double percentage;
    }

    // Additional response classes for specific endpoints

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCoursesResponse {
        private List<TopCourseItem> courses;
        private String sortedBy;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCourseItem {
        private Long courseId;
        private String name;
        private String creatorName;
        private Long totalEnrollments;
        private Long completedEnrollments;
        private Double averageRating;
        private Integer totalFeedbacks;
        private String imageUrl;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivitiesResponse {
        private List<ActivityItem> activities;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String activityType; // ENROLLMENT, REPORT, WARNING, COURSE_CREATED, etc.
        private String description;
        private String entityId;
        private String userId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;
        private String severity; // INFO, WARNING, CRITICAL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCreatorsResponse {
        private List<TopCreatorItem> creators;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCreatorItem {
        private String creatorId;
        private String fullName;
        private String imageUrl;
        private Double balance;
        private Long totalCourses;
        private Long publishedCourses;
        private Long totalEnrollments;
        private Double averageRating;
        private Integer reputationScore;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate joinDate;
    }
}