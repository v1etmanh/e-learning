package com.jpd.web.controller.admin;

import com.jpd.web.dto.AdminDashboardOverviewResponse;
import com.jpd.web.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Admin Dashboard Overview Controller
 * Provides comprehensive statistics and metrics for administrative dashboard
 *
 * @author E-Learning Platform Team
 * @version 1.0
 * @since 2026-02-08
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardOverviewController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Get comprehensive dashboard overview with all key metrics
     *
     * @param startDate Optional start date for filtering time-based metrics (format: yyyy-MM-dd)
     * @param endDate Optional end date for filtering time-based metrics (format: yyyy-MM-dd)
     * @return AdminDashboardOverviewResponse containing all dashboard metrics
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse>> getDashboardOverview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        log.info("Admin dashboard overview requested - startDate: {}, endDate: {}", startDate, endDate);

        AdminDashboardOverviewResponse overview = adminDashboardService.getDashboardOverview(startDate, endDate);

        ApiResponse<AdminDashboardOverviewResponse> response = ApiResponse.<AdminDashboardOverviewResponse>builder()
                .success(true)
                .message("Dashboard overview retrieved successfully")
                .data(overview)
                .build();

        log.info("Dashboard overview returned successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get user-related statistics
     *
     * @return User statistics including total, active, new, and creator counts
     */
    @GetMapping("/stats/users")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.UserStats>> getUserStats() {
        log.info("User statistics requested");

        AdminDashboardOverviewResponse.UserStats userStats = adminDashboardService.getUserStats();

        ApiResponse<AdminDashboardOverviewResponse.UserStats> response = ApiResponse.<AdminDashboardOverviewResponse.UserStats>builder()
                .success(true)
                .message("User statistics retrieved successfully")
                .data(userStats)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get course-related statistics
     *
     * @return Course statistics including total, published, banned, and pending courses
     */
    @GetMapping("/stats/courses")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.CourseStats>> getCourseStats() {
        log.info("Course statistics requested");

        AdminDashboardOverviewResponse.CourseStats courseStats = adminDashboardService.getCourseStats();

        ApiResponse<AdminDashboardOverviewResponse.CourseStats> response = ApiResponse.<AdminDashboardOverviewResponse.CourseStats>builder()
                .success(true)
                .message("Course statistics retrieved successfully")
                .data(courseStats)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get enrollment-related statistics
     *
     * @param days Number of days to look back for trends (default: 30)
     * @return Enrollment statistics with trends
     */
    @GetMapping("/stats/enrollments")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.EnrollmentStats>> getEnrollmentStats(
            @RequestParam(defaultValue = "30") int days
    ) {
        log.info("Enrollment statistics requested for last {} days", days);

        AdminDashboardOverviewResponse.EnrollmentStats enrollmentStats =
                adminDashboardService.getEnrollmentStats(days);

        ApiResponse<AdminDashboardOverviewResponse.EnrollmentStats> response =
                ApiResponse.<AdminDashboardOverviewResponse.EnrollmentStats>builder()
                        .success(true)
                        .message("Enrollment statistics retrieved successfully")
                        .data(enrollmentStats)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get content moderation statistics
     *
     * @return Moderation statistics including reports, warnings, and bans
     */
    @GetMapping("/stats/moderation")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.ModerationStats>> getModerationStats() {
        log.info("Moderation statistics requested");

        AdminDashboardOverviewResponse.ModerationStats moderationStats =
                adminDashboardService.getModerationStats();

        ApiResponse<AdminDashboardOverviewResponse.ModerationStats> response =
                ApiResponse.<AdminDashboardOverviewResponse.ModerationStats>builder()
                        .success(true)
                        .message("Moderation statistics retrieved successfully")
                        .data(moderationStats)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get engagement metrics
     *
     * @param days Number of days for engagement analysis (default: 30)
     * @return Engagement statistics
     */
    @GetMapping("/stats/engagement")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.EngagementStats>> getEngagementStats(
            @RequestParam(defaultValue = "30") int days
    ) {
        log.info("Engagement statistics requested for last {} days", days);

        AdminDashboardOverviewResponse.EngagementStats engagementStats =
                adminDashboardService.getEngagementStats(days);

        ApiResponse<AdminDashboardOverviewResponse.EngagementStats> response =
                ApiResponse.<AdminDashboardOverviewResponse.EngagementStats>builder()
                        .success(true)
                        .message("Engagement statistics retrieved successfully")
                        .data(engagementStats)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get system health and performance metrics
     *
     * @return System health statistics
     */
    @GetMapping("/stats/system-health")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.SystemHealthStats>> getSystemHealth() {
        log.info("System health statistics requested");

        AdminDashboardOverviewResponse.SystemHealthStats systemHealth =
                adminDashboardService.getSystemHealth();

        ApiResponse<AdminDashboardOverviewResponse.SystemHealthStats> response =
                ApiResponse.<AdminDashboardOverviewResponse.SystemHealthStats>builder()
                        .success(true)
                        .message("System health metrics retrieved successfully")
                        .data(systemHealth)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get top performing courses
     *
     * @param limit Number of top courses to return (default: 10)
     * @param sortBy Sorting criteria: enrollments, rating, revenue (default: enrollments)
     * @return List of top performing courses
     */
    @GetMapping("/top-courses")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.TopCoursesResponse>> getTopCourses(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "enrollments") String sortBy
    ) {
        log.info("Top courses requested - limit: {}, sortBy: {}", limit, sortBy);

        AdminDashboardOverviewResponse.TopCoursesResponse topCourses =
                adminDashboardService.getTopCourses(limit, sortBy);

        ApiResponse<AdminDashboardOverviewResponse.TopCoursesResponse> response =
                ApiResponse.<AdminDashboardOverviewResponse.TopCoursesResponse>builder()
                        .success(true)
                        .message("Top courses retrieved successfully")
                        .data(topCourses)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get recent activity feed
     *
     * @param limit Number of recent activities to return (default: 20)
     * @return List of recent activities
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.RecentActivitiesResponse>> getRecentActivities(
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("Recent activities requested - limit: {}", limit);

        AdminDashboardOverviewResponse.RecentActivitiesResponse activities =
                adminDashboardService.getRecentActivities(limit);

        ApiResponse<AdminDashboardOverviewResponse.RecentActivitiesResponse> response =
                ApiResponse.<AdminDashboardOverviewResponse.RecentActivitiesResponse>builder()
                        .success(true)
                        .message("Recent activities retrieved successfully")
                        .data(activities)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get creator performance metrics
     *
     * @param limit Number of top creators to return (default: 10)
     * @return List of top performing creators
     */
    @GetMapping("/top-creators")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.TopCreatorsResponse>> getTopCreators(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Top creators requested - limit: {}", limit);

        AdminDashboardOverviewResponse.TopCreatorsResponse topCreators =
                adminDashboardService.getTopCreators(limit);

        ApiResponse<AdminDashboardOverviewResponse.TopCreatorsResponse> response =
                ApiResponse.<AdminDashboardOverviewResponse.TopCreatorsResponse>builder()
                        .success(true)
                        .message("Top creators retrieved successfully")
                        .data(topCreators)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Export dashboard data to CSV or Excel
     *
     * @param format Export format: csv or excel (default: csv)
     * @param startDate Start date for data export (format: yyyy-MM-dd)
     * @param endDate End date for data export (format: yyyy-MM-dd)
     * @return Exported file as byte array
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDashboardData(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        log.info("Dashboard export requested - format: {}, startDate: {}, endDate: {}",
                format, startDate, endDate);

        byte[] exportData = adminDashboardService.exportDashboardData(format, startDate, endDate);

        String filename = "dashboard_export_" + LocalDate.now() + "." + format;

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .header("Content-Type", format.equals("excel") ?
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" :
                        "text/csv")
                .body(exportData);
    }
}