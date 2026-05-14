package com.jpd.web.repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Wishlist;


@Repository
public interface WishlistRepository extends CrudRepository<Wishlist,Long> {
Optional<Wishlist>findByCourse_CourseIdAndCustomerId(long courseId,String customerId);
List<Wishlist> findByCustomerId(String customerId);

    Long countByCustomerId(String customerId);

    Long countByCourse_CourseId(Long courseId);

    Long countByCreateAtAfter(Date date);

    // Find queries


    List<Wishlist> findByCourse_CourseId(Long courseId);

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.course WHERE w.customerId = :customerId")
    List<Wishlist> findByCustomerIdWithCourse(@Param("customerId") String customerId);

    @Query("SELECT w FROM Wishlist w WHERE w.customerId = :customerId AND w.course.courseId = :courseId")
    Wishlist findByCustomerIdAndCourseId(@Param("customerId") String customerId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.course.creator.creatorId = :creatorId")
    Long countByCreatorId(@Param("creatorId") String creatorId);
}
