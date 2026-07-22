package com.jpd.web.controller.admin;

import com.jpd.web.dto.AdminDashboardOverviewResponse;
import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - dashboard", description = """
        Platform-wide statistics for the administrative dashboard.

        Requires the `ADMIN` realm role, enforced both by the URL rule on `/api/admin/**` and by
        `@PreAuthorize` on this class. Every endpoint is read-only and returns the `ApiResponse` envelope
        from `com.jpd.web.controller.admin` — payload under `data`, plus `success`, `message` and
        `timestamp`. The one exception is the export endpoint, which returns a raw file.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class AdminDashboardOverviewController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Get comprehensive dashboard overview with all key metrics
     *
     * @param startDate Optional start date for filtering time-based metrics (format: yyyy-MM-dd)
     * @param endDate Optional end date for filtering time-based metrics (format: yyyy-MM-dd)
     * @return AdminDashboardOverviewResponse containing all dashboard metrics
     */
    @Operation(
        summary = "Get the full dashboard overview",
        description = """
            Returns every dashboard metric in one call: user, course, enrollment, moderation, engagement
            and system-health statistics, plus top courses, top creators and recent activity.

            `startDate` and `endDate` narrow the time-based metrics only; totals and counts are
            unaffected. Omit both for the all-time view. The range is not validated, so an `endDate`
            before `startDate` simply yields empty time-based figures.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All dashboard metrics, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A date was not in `yyyy-MM-dd` form (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse>> getDashboardOverview(
            @Parameter(description = "Start of the reporting window, `yyyy-MM-dd`. Applies to time-based metrics only.", example = "2026-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End of the reporting window, `yyyy-MM-dd`. Applies to time-based metrics only.", example = "2026-07-22")
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
    @Operation(
        summary = "Get user statistics",
        description = "Returns headline user numbers: total registered users, active users, new signups and how many are creators. Takes no parameters and covers the platform's whole history.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User statistics, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.UserStats.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(
        summary = "Get course statistics",
        description = "Returns catalogue-wide course numbers: totals, published versus unpublished, and banned counts. Takes no parameters.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course statistics, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.CourseStats.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(
        summary = "Get enrollment statistics",
        description = "Returns enrollment volume and completion figures over a trailing window. `days` is not range-checked, so a zero or negative value is passed straight through to the query.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment statistics, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.EnrollmentStats.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "`days` was not a valid integer (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stats/enrollments")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.EnrollmentStats>> getEnrollmentStats(
            @Parameter(description = "Size of the trailing window in days.", example = "30")
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
    @Operation(
        summary = "Get moderation statistics",
        description = "Returns the moderation workload: outstanding reports, pending certificate reviews, warnings issued and bans in force. Takes no parameters.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Moderation statistics, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.ModerationStats.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(
        summary = "Get engagement statistics",
        description = "Returns how actively learners are using the platform over a trailing window. `days` is not range-checked.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Engagement statistics, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.EngagementStats.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "`days` was not a valid integer (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stats/engagement")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.EngagementStats>> getEngagementStats(
            @Parameter(description = "Size of the trailing window in days.", example = "30")
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
    @Operation(
        summary = "Get system health statistics",
        description = "Returns operational health indicators for the dashboard's status panel. Takes no parameters. This is a dashboard tile, not a liveness probe — use the actuator endpoints for monitoring.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "System health statistics, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.SystemHealthStats.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(
        summary = "Get the best-performing courses",
        description = """
            Returns a leaderboard of courses. `sortBy` selects the ranking metric and is passed to the
            service as a free-form string — it is not validated against a fixed set here, so an
            unrecognised value falls back to whatever the service does by default.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Top courses, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.TopCoursesResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "`limit` was not a valid integer (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/top-courses")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.TopCoursesResponse>> getTopCourses(
            @Parameter(description = "How many courses to return.", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Ranking metric, for example `enrollments` or `rating`. Not validated against a fixed list.", example = "enrollments")
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
    @Operation(
        summary = "Get recent platform activity",
        description = "Returns the latest events across the platform — the dashboard's activity feed — newest first.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recent activity, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.RecentActivitiesResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "`limit` was not a valid integer (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/recent-activities")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.RecentActivitiesResponse>> getRecentActivities(
            @Parameter(description = "How many events to return.", example = "20")
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
    @Operation(
        summary = "Get the best-performing creators",
        description = "Returns a leaderboard of creators by platform contribution, for the dashboard's top-creators panel.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Top creators, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardOverviewResponse.TopCreatorsResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "`limit` was not a valid integer (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/top-creators")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse.TopCreatorsResponse>> getTopCreators(
            @Parameter(description = "How many creators to return.", example = "10")
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
    @Operation(
        summary = "Export dashboard data as a file",
        description = """
            Returns the dashboard data as a downloadable file rather than JSON, with a
            `Content-Disposition: attachment` header naming it `dashboard_export_<today>.<format>`.

            `format` accepts `csv` or `excel`. Anything else is still accepted: the response is served as
            `text/csv` but the filename extension echoes whatever you sent, so pass one of the two
            supported values.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The exported file. `text/csv`, or the Excel spreadsheet type when `format=excel`.",
            content = {
                @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary"))}),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A date was not in `yyyy-MM-dd` form (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDashboardData(
            @Parameter(description = "Output format: `csv` or `excel`.", example = "csv")
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = "Start of the export window, `yyyy-MM-dd`.", example = "2026-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "End of the export window, `yyyy-MM-dd`.", example = "2026-07-22")
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