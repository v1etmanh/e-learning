package com.jpd.web.service;

import com.jpd.web.dto.AdminDashboardOverviewResponse;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Dashboard Service Implementation
 * Provides business logic for calculating and aggregating dashboard statistics
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final CreatorRepository creatorRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReportRepository reportRepository;
    private final CreatorWarningRepository creatorWarningRepository;
    private final CommentRepository commentRepository;
    private final FeedbackRepository feedbackRepository;
    private final WishlistRepository wishlistRepository;
    private final CourseMetricsRepository courseMetricsRepository;

    /**
     * Get comprehensive dashboard overview
     */
    public AdminDashboardOverviewResponse getDashboardOverview(LocalDate startDate, LocalDate endDate) {
        log.info("Generating dashboard overview - startDate: {}, endDate: {}", startDate, endDate);
        
        // Set default date range if not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }
        
        String period = startDate.equals(endDate.minusDays(30)) ? 
                "Last 30 days" : 
                String.format("Custom: %s to %s", startDate, endDate);
        
        return AdminDashboardOverviewResponse.builder()
                .userStats(getUserStats())
                .courseStats(getCourseStats())
                .enrollmentStats(getEnrollmentStats(30))
                .revenueStats(getRevenueStats(startDate, endDate))
                .moderationStats(getModerationStats())
                .engagementStats(getEngagementStats(30))
                .systemHealthStats(getSystemHealth())
                .generatedAt(LocalDateTime.now())
                .period(period)
                .build();
    }

    /**
     * Calculate user statistics
     */
    public AdminDashboardOverviewResponse.UserStats getUserStats() {
        log.debug("Calculating user statistics");
        
        Long totalCreators = creatorRepository.count();
        Long bannedCreators = creatorRepository.countByBan(true);
        
        // Calculate time-based metrics
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        LocalDate monthAgo = today.minusDays(30);
        
        Long newCreatorsToday = creatorRepository.countByCreateDateAfter(
                java.sql.Date.valueOf(today));
        Long newCreatorsThisWeek = creatorRepository.countByCreateDateAfter(
                java.sql.Date.valueOf(weekAgo));
        Long newCreatorsThisMonth = creatorRepository.countByCreateDateAfter(
                java.sql.Date.valueOf(monthAgo));
        
        // Calculate growth rate (month-over-month)
        LocalDate twoMonthsAgo = monthAgo.minusDays(30);
        Long creatorsLastMonth = creatorRepository.countByCreateDateBetween(
                java.sql.Date.valueOf(twoMonthsAgo), 
                java.sql.Date.valueOf(monthAgo));
        
        Double growthRate = creatorsLastMonth > 0 ? 
                ((double) newCreatorsThisMonth / creatorsLastMonth) * 100 : 0.0;
        
        return AdminDashboardOverviewResponse.UserStats.builder()
                .totalUsers(totalCreators) // In this system, users are creators
                .totalCreators(totalCreators)
                .totalCustomers(0L) // Customer data would come from separate service
                .activeUsersLast30Days(newCreatorsThisMonth)
                .newUsersToday(newCreatorsToday)
                .newUsersThisWeek(newCreatorsThisWeek)
                .newUsersThisMonth(newCreatorsThisMonth)
                .userGrowthRate(growthRate)
                .bannedCreators(bannedCreators)
                .creatorsPendingApproval(creatorRepository.countByStatus(Status.PENDING))
                .dailyTrends(calculateDailyUserTrends(30))
                .build();
    }

    /**
     * Calculate course statistics
     */
    public AdminDashboardOverviewResponse.CourseStats getCourseStats() {
        log.debug("Calculating course statistics");
        
        Long totalCourses = courseRepository.count();
        Long publishedCourses = courseRepository.countByIsPublic(true);
        Long privateCourses = courseRepository.countByIsPublic(false);
        Long bannedCourses = courseRepository.countByIsBan(true);
        
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        LocalDate monthAgo = today.minusDays(30);
        
        Long coursesToday = courseRepository.countByCreatedAtAfter(today);
        Long coursesThisWeek = courseRepository.countByCreatedAtAfter(weekAgo);
        Long coursesThisMonth = courseRepository.countByCreatedAtAfter(monthAgo);
        
        Long totalCreators = creatorRepository.count();
        Double avgCoursesPerCreator = totalCreators > 0 ? 
                (double) totalCourses / totalCreators : 0.0;
        
        // Calculate growth rate
        LocalDate twoMonthsAgo = monthAgo.minusDays(30);
        Long coursesLastMonth = courseRepository.countByCreatedAtBetween(twoMonthsAgo, monthAgo);
        Double growthRate = coursesLastMonth > 0 ? 
                ((double) coursesThisMonth / coursesLastMonth) * 100 : 0.0;
        
        return AdminDashboardOverviewResponse.CourseStats.builder()
                .totalCourses(totalCourses)
                .publishedCourses(publishedCourses)
                .privateCourses(privateCourses)
                .bannedCourses(bannedCourses)
                .pendingApproval(0L) // Could be based on isPublic=false && !isBan
                .coursesCreatedToday(coursesToday)
                .coursesCreatedThisWeek(coursesThisWeek)
                .coursesCreatedThisMonth(coursesThisMonth)
                .averageCoursesPerCreator(avgCoursesPerCreator)
                .courseGrowthRate(growthRate)
                .coursesByLanguage(calculateCoursesByLanguage())
                .coursesByAccessMode(calculateCoursesByAccessMode())
                .build();
    }

    /**
     * Calculate enrollment statistics
     */
    public AdminDashboardOverviewResponse.EnrollmentStats getEnrollmentStats(int days) {
        log.debug("Calculating enrollment statistics for last {} days", days);
        
        Long totalEnrollments = enrollmentRepository.count();
        Long completedEnrollments = enrollmentRepository.countByIsFinish(true);
        Long activeEnrollments = totalEnrollments - completedEnrollments;
        
        Double completionRate = totalEnrollments > 0 ? 
                ((double) completedEnrollments / totalEnrollments) * 100 : 0.0;
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);
        
        Long enrollmentsToday = enrollmentRepository.countByCreateDateAfter(today);
        Long enrollmentsThisWeek = enrollmentRepository.countByCreateDateAfter(weekAgo);
        Long enrollmentsThisMonth = enrollmentRepository.countByCreateDateAfter(monthAgo);
        
        // Calculate growth rate
        LocalDateTime twoMonthsAgo = monthAgo.minusDays(30);
        Long enrollmentsLastMonth = enrollmentRepository.countByCreateDateBetween(
                twoMonthsAgo, monthAgo);
        Double growthRate = enrollmentsLastMonth > 0 ? 
                ((double) enrollmentsThisMonth / enrollmentsLastMonth) * 100 : 0.0;
        
        Long totalCourses = courseRepository.count();
        Double avgEnrollmentsPerCourse = totalCourses > 0 ? 
                (double) totalEnrollments / totalCourses : 0.0;
        
        return AdminDashboardOverviewResponse.EnrollmentStats.builder()
                .totalEnrollments(totalEnrollments)
                .activeEnrollments(activeEnrollments)
                .completedEnrollments(completedEnrollments)
                .completionRate(completionRate)
                .enrollmentsToday(enrollmentsToday)
                .enrollmentsThisWeek(enrollmentsThisWeek)
                .enrollmentsThisMonth(enrollmentsThisMonth)
                .enrollmentGrowthRate(growthRate)
                .averageEnrollmentsPerCourse(avgEnrollmentsPerCourse)
                .totalCourseCompletions(completedEnrollments)
                .dailyTrends(calculateDailyEnrollmentTrends(days))
                .build();
    }

    /**
     * Calculate revenue statistics
     */
    public AdminDashboardOverviewResponse.RevenueStats getRevenueStats(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating revenue statistics");
        
        Double totalBalance = creatorRepository.sumAllBalances();
        if (totalBalance == null) totalBalance = 0.0;
        
        Long totalCreators = creatorRepository.count();
        Double avgBalance = totalCreators > 0 ? totalBalance / totalCreators : 0.0;
        
        Double highestBalance = creatorRepository.findMaxBalance();
        if (highestBalance == null) highestBalance = 0.0;
        
        Long creatorsWithBalance = creatorRepository.countByBalanceGreaterThan(0.0);
        Long creatorsWithZeroBalance = totalCreators - creatorsWithBalance;
        
        // Calculate month-over-month revenue (using balance as proxy)
        Double totalBalanceThisMonth = totalBalance; // Simplified
        Double totalBalanceLastMonth = totalBalance * 0.9; // Simplified estimation
        
        Double revenueGrowthRate = totalBalanceLastMonth > 0 ? 
                ((totalBalanceThisMonth - totalBalanceLastMonth) / totalBalanceLastMonth) * 100 : 0.0;
        
        return AdminDashboardOverviewResponse.RevenueStats.builder()
                .totalCreatorBalance(totalBalance)
                .averageCreatorBalance(avgBalance)
                .highestCreatorBalance(highestBalance)
                .creatorsWithBalance(creatorsWithBalance)
                .creatorsWithZeroBalance(creatorsWithZeroBalance)
                .totalBalanceThisMonth(totalBalanceThisMonth)
                .totalBalanceLastMonth(totalBalanceLastMonth)
                .revenueGrowthRate(revenueGrowthRate)
                .topRevenueCreators(getTopRevenueCreators(5))
                .build();
    }

    /**
     * Calculate moderation statistics
     */
    public AdminDashboardOverviewResponse.ModerationStats getModerationStats() {
        log.debug("Calculating moderation statistics");
        
        Long totalReports = reportRepository.count();
        Long newReports = reportRepository.countByStatus("NEW");
        Long reviewingReports = reportRepository.countByStatus("REVIEWING");
        Long resolvedReports = reportRepository.countByStatus("RESOLVED");
        Long dismissedReports = reportRepository.countByStatus("DISMISSED");
        Long pendingReviewReports = newReports + reviewingReports;
        
        Long totalWarnings = creatorWarningRepository.count();
        Long activeWarnings = creatorWarningRepository.countByIsActive(true);
        
        Long bannedCreators = creatorRepository.countByBan(true);
        Long bannedCourses = courseRepository.countByIsBan(true);
        
        Long totalCourses = courseRepository.count();
        Double avgReportsPerCourse = totalCourses > 0 ? 
                (double) totalReports / totalCourses : 0.0;
        
        return AdminDashboardOverviewResponse.ModerationStats.builder()
                .totalReports(totalReports)
                .newReports(newReports)
                .reviewingReports(reviewingReports)
                .resolvedReports(resolvedReports)
                .dismissedReports(dismissedReports)
                .pendingReportReview(pendingReviewReports)
                .totalWarnings(totalWarnings)
                .activeWarnings(activeWarnings)
                .bannedCreators(bannedCreators)
                .bannedCourses(bannedCourses)
                .averageReportsPerCourse(avgReportsPerCourse)
                .reportsByType(calculateReportsByType())
                .recentReports(getRecentReports(5))
                .build();
    }

    /**
     * Calculate engagement statistics
     */
    public AdminDashboardOverviewResponse.EngagementStats getEngagementStats(int days) {
        log.debug("Calculating engagement statistics for last {} days", days);
        
        Long totalComments = commentRepository.count();
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);
        
        Long commentsToday = commentRepository.countByCreateAtAfter(today);
        Long commentsThisWeek = commentRepository.countByCreateAtAfter(weekAgo);
        Long commentsThisMonth = commentRepository.countByCreateAtAfter(monthAgo);
        
        Long totalFeedbacks = feedbackRepository.count();
        Long feedbacksThisMonth = feedbackRepository.countByEnrollmentCreateDateAfter(monthAgo);
        
        Double averageRating = feedbackRepository.calculateAverageRating();
        if (averageRating == null) averageRating = 0.0;
        
        Long totalRatings = totalFeedbacks; // Each feedback has a rating
        
        Long totalWishlists = wishlistRepository.count();
        Long wishlistsThisMonth = wishlistRepository.countByCreateAtAfter(
                java.sql.Date.valueOf(monthAgo.toLocalDate()));
        
        Long totalCourses = courseRepository.count();
        Double avgCommentsPerCourse = totalCourses > 0 ? 
                (double) totalComments / totalCourses : 0.0;
        Double avgFeedbacksPerCourse = totalCourses > 0 ? 
                (double) totalFeedbacks / totalCourses : 0.0;
        
        return AdminDashboardOverviewResponse.EngagementStats.builder()
                .totalComments(totalComments)
                .commentsToday(commentsToday)
                .commentsThisWeek(commentsThisWeek)
                .commentsThisMonth(commentsThisMonth)
                .totalFeedbacks(totalFeedbacks)
                .feedbacksThisMonth(feedbacksThisMonth)
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .totalWishlists(totalWishlists)
                .wishlistsThisMonth(wishlistsThisMonth)
                .averageCommentsPerCourse(avgCommentsPerCourse)
                .averageFeedbacksPerCourse(avgFeedbacksPerCourse)
                .ratingDistribution(calculateRatingDistribution())
                .build();
    }

    /**
     * Calculate system health metrics
     */
    public AdminDashboardOverviewResponse.SystemHealthStats getSystemHealth() {
        log.debug("Calculating system health metrics");
        
        Long totalRecords = creatorRepository.count() + 
                            courseRepository.count() + 
                            enrollmentRepository.count();
        
        Long totalMetrics = courseMetricsRepository.count();
        Long totalCourses = courseRepository.count();
        Long coursesWithoutMetrics = totalCourses - totalMetrics;
        
        Long creatorsWithoutCourses = creatorRepository.countCreatorsWithoutCourses();
        
        // Calculate health score (0-100)
        double healthScore = 100.0;
        if (coursesWithoutMetrics > 0) {
            healthScore -= (coursesWithoutMetrics * 1.0 / totalCourses) * 20;
        }
        if (creatorsWithoutCourses > 0) {
            healthScore -= 10;
        }
        
        String status = healthScore >= 90 ? "HEALTHY" : 
                       healthScore >= 70 ? "WARNING" : "CRITICAL";
        
        return AdminDashboardOverviewResponse.SystemHealthStats.builder()
                .totalDatabaseRecords(totalRecords)
                .totalCourseMetrics(totalMetrics)
                .orphanedEnrollments(0L) // Would require complex query
                .coursesWithoutMetrics(coursesWithoutMetrics)
                .creatorsWithoutCourses(creatorsWithoutCourses)
                .databaseHealthScore(healthScore)
                .status(status)
                .lastDataSync(LocalDateTime.now())
                .build();
    }

    /**
     * Get top performing courses
     */
    public AdminDashboardOverviewResponse.TopCoursesResponse getTopCourses(int limit, String sortBy) {
        log.debug("Getting top {} courses sorted by {}", limit, sortBy);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Course> topCourses;
        
        switch (sortBy.toLowerCase()) {
            case "rating":
                topCourses = courseRepository.findTopByRating(pageable);
                break;
            case "revenue":
                topCourses = courseRepository.findTopByRevenue(pageable);
                break;
            case "enrollments":
            default:
                topCourses = courseRepository.findTopByEnrollments(pageable);
                break;
        }
        
        List<AdminDashboardOverviewResponse.TopCourseItem> courseItems = topCourses.stream()
                .map(this::mapToTopCourseItem)
                .collect(Collectors.toList());
        
        return AdminDashboardOverviewResponse.TopCoursesResponse.builder()
                .courses(courseItems)
                .sortedBy(sortBy)
                .limit(limit)
                .build();
    }

    /**
     * Get recent platform activities
     */
    public AdminDashboardOverviewResponse.RecentActivitiesResponse getRecentActivities(int limit) {
        log.debug("Getting {} recent activities", limit);
        
        List<AdminDashboardOverviewResponse.ActivityItem> activities = new ArrayList<>();
        
        // Get recent enrollments
        Pageable enrollmentPageable = PageRequest.of(
                0,
                Math.max(1, limit / 3),
                Sort.by("createDate").descending()
        );

// Report pageable
        Pageable reportPageable = PageRequest.of(
                0,
                Math.max(1, limit / 3),
                Sort.by("createdAt").descending()
        );
         List<Enrollment> recentEnrollments = enrollmentRepository.findTopByOrderByCreateDateDesc(enrollmentPageable);
        
        for (Enrollment enrollment : recentEnrollments) {
            activities.add(AdminDashboardOverviewResponse.ActivityItem.builder()
                    .activityType("ENROLLMENT")
                    .description("New enrollment in " + enrollment.getCourse().getName())
                    .entityId(String.valueOf(enrollment.getEnrollId()))
                    .userId(enrollment.getCustomerId())
                    .timestamp(enrollment.getCreateDate())
                    .severity("INFO")
                    .build());
        }
        
        // Get recent reports
        List<Report> recentReports = reportRepository.findRecentReports(reportPageable);
        for (Report report : recentReports) {
            activities.add(AdminDashboardOverviewResponse.ActivityItem.builder()
                    .activityType("REPORT")
                    .description("New report: " + report.getType())
                    .entityId(String.valueOf(report.getReportId()))
                    .userId(report.getCustomerId())
                    .timestamp(report.getCreatedAt())
                    .severity("WARNING")
                    .build());
        }
        
        // Get recent warnings
        List<CreatorWarning> recentWarnings = creatorWarningRepository.findTopByOrderByIssuedAtDesc(enrollmentPageable);
        for (CreatorWarning warning : recentWarnings) {
            activities.add(AdminDashboardOverviewResponse.ActivityItem.builder()
                    .activityType("WARNING")
                    .description("Warning issued to creator")
                    .entityId(String.valueOf(warning.getWarningId()))
                    .userId(warning.getCreator().getCreatorId())
                    .timestamp(warning.getIssuedAt())
                    .severity("CRITICAL")
                    .build());
        }
        
        // Sort all activities by timestamp and limit
        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        if (activities.size() > limit) {
            activities = activities.subList(0, limit);
        }
        
        return AdminDashboardOverviewResponse.RecentActivitiesResponse.builder()
                .activities(activities)
                .limit(limit)
                .build();
    }

    /**
     * Get top creators
     */
    public AdminDashboardOverviewResponse.TopCreatorsResponse getTopCreators(int limit) {
        log.debug("Getting top {} creators", limit);
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by("balance").descending());
        List<Creator> topCreators = creatorRepository.findTopByOrderByBalanceDesc(pageable);
        
        List<AdminDashboardOverviewResponse.TopCreatorItem> creatorItems = topCreators.stream()
                .map(this::mapToTopCreatorItem)
                .collect(Collectors.toList());
        
        return AdminDashboardOverviewResponse.TopCreatorsResponse.builder()
                .creators(creatorItems)
                .limit(limit)
                .build();
    }

    /**
     * Export dashboard data
     */
    public byte[] exportDashboardData(String format, LocalDate startDate, LocalDate endDate) {
        log.debug("Exporting dashboard data in {} format", format);
        
        AdminDashboardOverviewResponse data = getDashboardOverview(startDate, endDate);
        
        if ("csv".equalsIgnoreCase(format)) {
            return exportToCSV(data);
        } else {
            return exportToExcel(data);
        }
    }

    // Helper methods

    private List<AdminDashboardOverviewResponse.DailyUserTrend> calculateDailyUserTrends(int days) {
        List<AdminDashboardOverviewResponse.DailyUserTrend> trends = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Long newUsers = creatorRepository.countByCreateDateEquals(java.sql.Date.valueOf(date));
            
            trends.add(AdminDashboardOverviewResponse.DailyUserTrend.builder()
                    .date(date)
                    .newUsers(newUsers)
                    .activeUsers(newUsers) // Simplified
                    .build());
        }
        
        return trends;
    }

    private List<AdminDashboardOverviewResponse.DailyEnrollmentTrend> calculateDailyEnrollmentTrends(int days) {
        List<AdminDashboardOverviewResponse.DailyEnrollmentTrend> trends = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            Long enrollments = enrollmentRepository.countByCreateDateBetween(startOfDay, endOfDay);
            Long completions = enrollmentRepository.countCompletedByDateBetween(startOfDay, endOfDay);
            
            trends.add(AdminDashboardOverviewResponse.DailyEnrollmentTrend.builder()
                    .date(date)
                    .enrollments(enrollments)
                    .completions(completions)
                    .build());
        }
        
        return trends;
    }

    private List<AdminDashboardOverviewResponse.CourseByLanguage> calculateCoursesByLanguage() {
        List<Object[]> languageCounts = courseRepository.countByLanguageGroupBy();
        Long totalCourses = courseRepository.count();

        return languageCounts.stream()
                .map(row -> AdminDashboardOverviewResponse.CourseByLanguage.builder()
                        .language((String) row[0])  // Đã được CAST sang String
                        .count((Long) row[1])
                        .percentage(((Long) row[1] * 100.0) / totalCourses)
                        .build())
                .collect(Collectors.toList());
    }

    private List<AdminDashboardOverviewResponse.CourseByAccessMode> calculateCoursesByAccessMode() {
        List<Object[]> accessModeCounts = courseRepository.countByAccessModeGroupBy();
        Long totalCourses = courseRepository.count();
        
        return accessModeCounts.stream()
                .map(entry -> AdminDashboardOverviewResponse.CourseByAccessMode.builder()
                        .accessMode((String) entry[0])
                        .count((Long) entry[1])
                        .percentage(((Long) entry[1] * 100.0) / totalCourses)
                        .build())
                .collect(Collectors.toList());
    }

    private List<AdminDashboardOverviewResponse.TopRevenueCreator> getTopRevenueCreators(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("balance").descending());
        return creatorRepository.findTopByOrderByBalanceDesc(pageable).stream()
                .map(creator -> AdminDashboardOverviewResponse.TopRevenueCreator.builder()
                        .creatorId(creator.getCreatorId())
                        .fullName(creator.getFullName())
                        .balance(creator.getBalance())
                        .totalCourses(creator.getCourses() != null ? (long) creator.getCourses().size() : 0L)
                        .totalStudents(enrollmentRepository.countByCreatorId(creator.getCreatorId()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<AdminDashboardOverviewResponse.ReportByType> calculateReportsByType() {
        List<Object[]> typeCounts = reportRepository.countByTypeGroupBy();
        Long totalReports = reportRepository.count();

        return typeCounts.stream()
                .map(entry -> AdminDashboardOverviewResponse.ReportByType.builder()
                        .reportType((String) entry[0])
                        .count((Long) entry[1])
                        .percentage((((Long) entry[1] )* 100.0) / totalReports)
                        .build())
                        .collect(Collectors.toList());
    }

    private List<AdminDashboardOverviewResponse.RecentReport> getRecentReports(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return reportRepository.findRecentReports(pageable).stream()
                .map(report -> AdminDashboardOverviewResponse.RecentReport.builder()
                        .reportId(report.getReportId())
                        .reportType(report.getType().toString())
                        .courseName(report.getCourse() != null ? report.getCourse().getName() : "N/A")
                        .status(report.getStatus())
                        .createdAt(report.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<AdminDashboardOverviewResponse.RatingDistribution> calculateRatingDistribution() {
        Map<Integer, Long> ratingCounts = feedbackRepository.countByRatingGroupBy();
        Long totalRatings = feedbackRepository.count();
        
        List<AdminDashboardOverviewResponse.RatingDistribution> distribution = new ArrayList<>();
        for (int rating = 1; rating <= 5; rating++) {
            Long count = ratingCounts.getOrDefault(rating, 0L);
            distribution.add(AdminDashboardOverviewResponse.RatingDistribution.builder()
                    .rating(rating)
                    .count(count)
                    .percentage(totalRatings > 0 ? (count * 100.0) / totalRatings : 0.0)
                    .build());
        }
        
        return distribution;
    }

    private AdminDashboardOverviewResponse.TopCourseItem mapToTopCourseItem(Course course) {
        Long enrollmentCount = course.getEnrollments() != null ? 
                (long) course.getEnrollments().size() : 0L;
        Long completionCount = enrollmentRepository.countCompletedByCourseId(course.getCourseId());
        
        Double avgRating = 0.0;
        Integer feedbackCount = 0;
        if (course.getCourseMetrics() != null) {
            avgRating = course.getCourseMetrics().getAverageRating();
            feedbackCount = course.getCourseMetrics().getTotalFeedbacks();
        }
        
        return AdminDashboardOverviewResponse.TopCourseItem.builder()
                .courseId(course.getCourseId())
                .name(course.getName())
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : "Unknown")
                .totalEnrollments(enrollmentCount)
                .completedEnrollments(completionCount)
                .averageRating(avgRating)
                .totalFeedbacks(feedbackCount)
                .imageUrl(course.getUrlImg())
                .createdAt(course.getCreatedAt())
                .build();
    }

    private AdminDashboardOverviewResponse.TopCreatorItem mapToTopCreatorItem(Creator creator) {
        Long totalCourses = creator.getCourses() != null ? (long) creator.getCourses().size() : 0L;
        Long publishedCourses = courseRepository.countByCreatorAndIsPublic(creator, true);
        Long totalEnrollments = enrollmentRepository.countByCreatorId(creator.getCreatorId());
        Double avgRating = courseMetricsRepository.calculateAverageRatingByCreator(creator.getCreatorId());
        
        return AdminDashboardOverviewResponse.TopCreatorItem.builder()
                .creatorId(creator.getCreatorId())
                .fullName(creator.getFullName())
                .imageUrl(creator.getImageUrl())
                .balance(creator.getBalance())
                .totalCourses(totalCourses)
                .publishedCourses(publishedCourses)
                .totalEnrollments(totalEnrollments)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .reputationScore(creator.getReputationScore())
                .joinDate(creator.getCreateDate().toLocalDate())
                .build();
    }

    private byte[] exportToCSV(AdminDashboardOverviewResponse data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println("Admin Dashboard Export - " + data.getGeneratedAt());
            writer.println("Period: " + data.getPeriod());
            writer.println();
            
            // User Stats
            writer.println("USER STATISTICS");
            writer.println("Total Users," + data.getUserStats().getTotalUsers());
            writer.println("Total Creators," + data.getUserStats().getTotalCreators());
            writer.println("Banned Creators," + data.getUserStats().getBannedCreators());
            writer.println();
            
            // Course Stats
            writer.println("COURSE STATISTICS");
            writer.println("Total Courses," + data.getCourseStats().getTotalCourses());
            writer.println("Published Courses," + data.getCourseStats().getPublishedCourses());
            writer.println("Banned Courses," + data.getCourseStats().getBannedCourses());
            writer.println();
            
            // Enrollment Stats
            writer.println("ENROLLMENT STATISTICS");
            writer.println("Total Enrollments," + data.getEnrollmentStats().getTotalEnrollments());
            writer.println("Completed Enrollments," + data.getEnrollmentStats().getCompletedEnrollments());
            writer.println("Completion Rate," + data.getEnrollmentStats().getCompletionRate() + "%");
            writer.println();
            
            writer.flush();
        }
        return baos.toByteArray();
    }

    private byte[] exportToExcel(AdminDashboardOverviewResponse data) {
        // For Excel export, you would use Apache POI
        // This is a placeholder - implement with Apache POI when needed
        return exportToCSV(data);
    }
}