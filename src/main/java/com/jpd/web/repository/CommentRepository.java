package com.jpd.web.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long>{
	List<Comment> findByCourse_CourseId(long courseId);

	Long countByCreateAtAfter(LocalDateTime date);

	Long countByCustomerId(String customerId);

	Long countByCourse_CourseId(Long courseId);

	// Find queries
	List<Comment> findByCustomerId(String customerId);

	List<Comment> findByCourse_CourseId(Long courseId);

	List<Comment> findByCreateAtBetween(LocalDateTime startDate, LocalDateTime endDate);

	@Query("SELECT c FROM Comment c ORDER BY c.createAt DESC")
	List<Comment> findAllOrderByCreateAtDesc(Pageable pageable);

	@Query("SELECT c FROM Comment c LEFT JOIN FETCH c.course WHERE c.commentId = :commentId")
	Comment findByIdWithCourse(@Param("commentId") Long commentId);

	@Query("SELECT c FROM Comment c WHERE c.course.courseId = :courseId ORDER BY c.createAt DESC")
	List<Comment> findByCourseIdOrderByCreateAtDesc(@Param("courseId") Long courseId);
}
