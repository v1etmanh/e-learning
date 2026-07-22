package com.jpd.web.service;

import com.jpd.web.dto.AdminDashboardOverviewResponse;
import com.jpd.web.model.Comment;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.CreatorWarning;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Report;
import com.jpd.web.model.ReportType;
import com.jpd.web.repository.CommentRepository;
import com.jpd.web.repository.CourseMetricsRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CreatorWarningRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.FeedbackRepository;
import com.jpd.web.repository.ReportRepository;
import com.jpd.web.repository.WishlistRepository;
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import com.jpd.web.testutil.EnrollmentTestDataBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private CreatorWarningRepository creatorWarningRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private CourseMetricsRepository courseMetricsRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Nested
    class UserStats {

        @Test
        void growthRate_isZero_whenNoCreatorsLastMonth() {
            when(creatorRepository.countByCreateDateBetween(any(), any())).thenReturn(0L);
            when(creatorRepository.countByCreateDateAfter(any())).thenReturn(5L);

            AdminDashboardOverviewResponse.UserStats stats = adminDashboardService.getUserStats();

            assertThat(stats.getUserGrowthRate()).isEqualTo(0.0);
        }

        @Test
        void growthRate_computedAsPercentage_whenCreatorsLastMonthPositive() {
            when(creatorRepository.countByCreateDateBetween(any(), any())).thenReturn(10L);
            when(creatorRepository.countByCreateDateAfter(any())).thenReturn(15L);

            AdminDashboardOverviewResponse.UserStats stats = adminDashboardService.getUserStats();

            assertThat(stats.getUserGrowthRate()).isCloseTo(150.0, within(1e-9));
        }
    }

    @Test
    void getRevenueStats_appliesNinetyPercentLastMonthEstimate() {
        when(creatorRepository.sumAllBalances()).thenReturn(1000.0);
        when(creatorRepository.count()).thenReturn(4L);

        AdminDashboardOverviewResponse.RevenueStats stats =
                adminDashboardService.getRevenueStats(LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(stats.getTotalBalanceLastMonth()).isCloseTo(900.0, within(1e-9));
        assertThat(stats.getRevenueGrowthRate()).isCloseTo((1000.0 - 900.0) / 900.0 * 100, within(1e-6));
    }

    @Nested
    class ModerationStats {

        @Test
        void pendingReportReview_isSumOfNewAndReviewing() {
            when(reportRepository.countByStatus("NEW")).thenReturn(3L);
            when(reportRepository.countByStatus("REVIEWING")).thenReturn(2L);

            AdminDashboardOverviewResponse.ModerationStats stats = adminDashboardService.getModerationStats();

            assertThat(stats.getPendingReportReview()).isEqualTo(5L);
        }

        @Test
        void averageReportsPerCourse_isZero_whenNoCourses() {
            when(courseRepository.count()).thenReturn(0L);
            when(reportRepository.count()).thenReturn(10L);

            AdminDashboardOverviewResponse.ModerationStats stats = adminDashboardService.getModerationStats();

            assertThat(stats.getAverageReportsPerCourse()).isEqualTo(0.0);
        }
    }

    @Nested
    class SystemHealth {

        @Test
        void healthy_whenNoMissingMetricsAndAllCreatorsHaveCourses() {
            when(courseRepository.count()).thenReturn(10L);
            when(courseMetricsRepository.count()).thenReturn(10L);
            when(creatorRepository.countCreatorsWithoutCourses()).thenReturn(0L);

            AdminDashboardOverviewResponse.SystemHealthStats stats = adminDashboardService.getSystemHealth();

            assertThat(stats.getDatabaseHealthScore()).isEqualTo(100.0);
            assertThat(stats.getStatus()).isEqualTo("HEALTHY");
        }

        @Test
        void warning_whenSomeCoursesLackMetricsAndSomeCreatorsLackCourses() {
            when(courseRepository.count()).thenReturn(10L);
            when(courseMetricsRepository.count()).thenReturn(8L); // 2 without metrics -> -4
            when(creatorRepository.countCreatorsWithoutCourses()).thenReturn(1L); // -10

            AdminDashboardOverviewResponse.SystemHealthStats stats = adminDashboardService.getSystemHealth();

            assertThat(stats.getDatabaseHealthScore()).isCloseTo(86.0, within(1e-9));
            assertThat(stats.getStatus()).isEqualTo("WARNING");
        }

        @Test
        void worstCaseDeduction_neverDropsBelowSeventy_criticalIsUnreachable() {
            // All courses lack metrics (max -20) AND creators without courses (max -10):
            // floor score is 70, which the ">= 70" branch classifies as WARNING, not CRITICAL.
            // This documents that the CRITICAL branch is currently unreachable via this formula.
            when(courseRepository.count()).thenReturn(10L);
            when(courseMetricsRepository.count()).thenReturn(0L);
            when(creatorRepository.countCreatorsWithoutCourses()).thenReturn(5L);

            AdminDashboardOverviewResponse.SystemHealthStats stats = adminDashboardService.getSystemHealth();

            assertThat(stats.getDatabaseHealthScore()).isCloseTo(70.0, within(1e-9));
            assertThat(stats.getStatus()).isEqualTo("WARNING");
        }
    }

    @Nested
    class GetTopCourses {

        @Test
        void dispatchesToFindTopByRating() {
            when(courseRepository.findTopByRating(any(Pageable.class))).thenReturn(List.of());

            adminDashboardService.getTopCourses(5, "rating");

            verify(courseRepository).findTopByRating(any(Pageable.class));
        }

        @Test
        void dispatchesToFindTopByRevenue() {
            when(courseRepository.findTopByRevenue(any(Pageable.class))).thenReturn(List.of());

            adminDashboardService.getTopCourses(5, "revenue");

            verify(courseRepository).findTopByRevenue(any(Pageable.class));
        }

        @Test
        void dispatchesToFindTopByEnrollments_forRecognizedValue() {
            when(courseRepository.findTopByEnrollments(any(Pageable.class))).thenReturn(List.of());

            adminDashboardService.getTopCourses(5, "enrollments");

            verify(courseRepository).findTopByEnrollments(any(Pageable.class));
        }

        @Test
        void dispatchesToFindTopByEnrollments_forUnrecognizedValue_defaultBranch() {
            when(courseRepository.findTopByEnrollments(any(Pageable.class))).thenReturn(List.of());

            adminDashboardService.getTopCourses(5, "totally-unknown-sort");

            verify(courseRepository).findTopByEnrollments(any(Pageable.class));
        }
    }

    @Test
    void getRecentActivities_mergesSortsDescendingAndTruncatesToLimit() {
        LocalDateTime t1 = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 7, 10, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 7, 20, 0, 0);

        Course course = CourseTestDataBuilder.aCourse().name("Course A").build();
        Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollmentOf("student-1", course);
        enrollment.setCreateDate(t1);

        Report report = Report.builder().reportId(1L).type(ReportType.OTHER).customerId("student-2")
                .createdAt(t2).build();

        Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
        CreatorWarning warning = CreatorWarning.builder().warningId(1L).creator(creator).issuedAt(t3).build();

        when(enrollmentRepository.findTopByOrderByCreateDateDesc(any(Pageable.class)))
                .thenReturn(List.of(enrollment));
        when(reportRepository.findRecentReports(any(Pageable.class))).thenReturn(List.of(report));
        when(creatorWarningRepository.findTopByOrderByIssuedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(warning));

        AdminDashboardOverviewResponse.RecentActivitiesResponse result =
                adminDashboardService.getRecentActivities(2);

        assertThat(result.getActivities()).hasSize(2);
        assertThat(result.getActivities().get(0).getActivityType()).isEqualTo("WARNING");
        assertThat(result.getActivities().get(1).getActivityType()).isEqualTo("REPORT");
    }

    @Test
    void exportDashboardData_nonCsvFormatCurrentlyReturnsSameBytesAsCsv() {
        byte[] csvBytes = adminDashboardService.exportDashboardData("csv", null, null);
        byte[] xlsxBytes = adminDashboardService.exportDashboardData("xlsx", null, null);

        assertThat(csvBytes).isNotEmpty();
        // exportToExcel is currently just a passthrough to exportToCSV.
        assertThat(new String(xlsxBytes)).startsWith("Admin Dashboard Export");
        assertThat(new String(csvBytes)).startsWith("Admin Dashboard Export");
    }
}
