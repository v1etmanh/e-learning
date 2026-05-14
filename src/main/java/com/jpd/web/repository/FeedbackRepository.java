package com.jpd.web.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public interface FeedbackRepository extends CrudRepository<Feedback,Long> {
	Optional<Feedback> findByEnrollment(Enrollment enrollment);

	Long countByRate(int rate);

	@Query("SELECT COUNT(f) FROM Feedback f WHERE f.enrollment.createDate >= :date")
	Long countByEnrollmentCreateDateAfter(@Param("date") LocalDateTime date);

	@Query("SELECT COUNT(f) FROM Feedback f WHERE f.enrollment.course.courseId = :courseId")
	Long countByCourseId(@Param("courseId") Long courseId);

	// Aggregate queries
	@Query("SELECT AVG(f.rate) FROM Feedback f")
	Double calculateAverageRating();

	@Query("SELECT AVG(f.rate) FROM Feedback f WHERE f.enrollment.course.courseId = :courseId")
	Double calculateAverageRatingByCourseId(@Param("courseId") Long courseId);

	@Query("SELECT f.rate, COUNT(f) FROM Feedback f GROUP BY f.rate")
	Map<Integer, Long> countByRatingGroupBy();

	// Find queries
	List<Feedback> findByRate(int rate);

	@Query("SELECT f FROM Feedback f WHERE f.enrollment.course.courseId = :courseId")
	List<Feedback> findByCourseId(@Param("courseId") Long courseId);

	@Query("SELECT f FROM Feedback f WHERE f.enrollment.customerId = :customerId")
	List<Feedback> findByCustomerId(@Param("customerId") String customerId);

	@Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.enrollment WHERE f.feedbackId = :feedbackId")
	Feedback findByIdWithEnrollment(@Param("feedbackId") Long feedbackId);

	@Query("SELECT f FROM Feedback f WHERE f.enrollment.course.creator.creatorId = :creatorId")
	List<Feedback> findByCreatorId(@Param("creatorId") String creatorId);
}
