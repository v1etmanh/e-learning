package com.jpd.web.repository;

import com.jpd.web.model.Report;
import com.jpd.web.model.ReportType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r JOIN r.course c WHERE c.creator.creatorId = :creatorId")
    List<Report> findByCreator_CreatorId(@Param("creatorId") String creatorId);

    List<Report> findByStatus(String status);

    @Query("SELECT COUNT(r) FROM Report r JOIN r.course c WHERE c.creator.creatorId = :creatorId AND r.createdAt >= :since")
    Long countByCreator_CreatorIdAndCreatedAtAfter(
            @Param("creatorId") Long creatorId,
            @Param("since") LocalDateTime since);


    Long countByStatus(String status);

    Long countByType(ReportType type);

    Long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT CAST(r.type AS string), COUNT(r) " +
            "FROM Report r " +
            "GROUP BY r.type")
    List<Object[]> countByTypeGroupBy();

    // Find queries

    List<Report> findByType(ReportType type);

    List<Report> findByCustomerId(String customerId);

    List<Report> findByCourse_CourseId(Long courseId);

    @Query("SELECT r FROM Report r ORDER BY r.createdAt DESC")
    List<Report> findRecentReports(Pageable pageable);
    @Query("SELECT r FROM Report r WHERE r.status = 'NEW' OR r.status = 'REVIEWING'")
    List<Report> findPendingReports();

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.course WHERE r.reportId = :reportId")
    Report findByIdWithCourse(@Param("reportId") Long reportId);

    @Query("SELECT r FROM Report r WHERE r.reviewedByAdmin = :adminId")
    List<Report> findByReviewedByAdmin(@Param("adminId") String adminId);

    @Query("SELECT r FROM Report r WHERE r.course.creator.creatorId = :creatorId")
    List<Report> findReportsByCreatorId(@Param("creatorId") String creatorId);
}
