package com.jpd.web.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>{
 List<Enrollment> findByCourse(Course course);

 List<Enrollment> findByCustomerId(String customerId);

 Optional<Enrollment> findByCourse_CourseIdAndCustomerId(long courseId, String customerId);

 //
 Long countByIsFinish(boolean isFinish);

 Long countByCreateDateAfter(LocalDateTime date);

 Long countByCreateDateBetween(LocalDateTime startDate, LocalDateTime endDate);

 @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.courseId = :courseId AND e.isFinish = true")
 Long countCompletedByCourseId(@Param("courseId") Long courseId);

 @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.isFinish = true AND e.createDate BETWEEN :startDate AND :endDate")
 Long countCompletedByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

 @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.creator.creatorId = :creatorId")
 Long countByCreatorId(@Param("creatorId") String creatorId);

 // Find queries

 List<Enrollment> findByCourse_CourseId(Long courseId);

 List<Enrollment> findByIsFinish(boolean isFinish);

 List<Enrollment> findTopByOrderByCreateDateDesc(Pageable pageable);

 @Query("SELECT e FROM Enrollment e LEFT JOIN FETCH e.course WHERE e.customerId = :customerId")
 List<Enrollment> findByCustomerIdWithCourse(@Param("customerId") String customerId);

 @Query("SELECT e FROM Enrollment e LEFT JOIN FETCH e.feedback WHERE e.enrollId = :enrollId")
 Enrollment findByIdWithFeedback(@Param("enrollId") Long enrollId);

 @Query("SELECT e FROM Enrollment e WHERE e.course.creator.creatorId = :creatorId")
 List<Enrollment> findByCreatorId(@Param("creatorId") String creatorId);

 @Query("SELECT e FROM Enrollment e WHERE e.createDate >= :date AND e.isFinish = false")
 List<Enrollment> findActiveEnrollmentsAfter(@Param("date") LocalDateTime date);
}
