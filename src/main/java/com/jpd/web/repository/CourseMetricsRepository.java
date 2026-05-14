package com.jpd.web.repository;

import com.jpd.web.model.CourseMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseMetricsRepository extends JpaRepository<CourseMetrics, Long> {
    Optional<CourseMetrics> findByCourse_CourseId(Long courseId);


    @Query("SELECT cm FROM CourseMetrics cm WHERE cm.course.creator.creatorId = :creatorId")
    List<CourseMetrics> findByCreatorId(@Param("creatorId") String creatorId);

    @Query("SELECT AVG(cm.averageRating) FROM CourseMetrics cm WHERE cm.course.creator.creatorId = :creatorId")
    Double calculateAverageRatingByCreator(@Param("creatorId") String creatorId);

    @Query("SELECT cm FROM CourseMetrics cm ORDER BY cm.averageRating DESC, cm.totalFeedbacks DESC")
    List<CourseMetrics> findTopRatedCourses();

    @Query("SELECT cm FROM CourseMetrics cm ORDER BY cm.totalStudents DESC")
    List<CourseMetrics> findMostPopularCourses();

    @Query("SELECT SUM(cm.totalStudents) FROM CourseMetrics cm WHERE cm.course.creator.creatorId = :creatorId")
    Long sumTotalStudentsByCreatorId(@Param("creatorId") String creatorId);
}