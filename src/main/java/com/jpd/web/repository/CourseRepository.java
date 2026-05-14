package com.jpd.web.repository;

import com.jpd.web.model.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Language;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseRepository  extends JpaRepository<Course, Long>{
List<Course> findByAccessMode(AccessMode accessMode);
@Query("SELECT DISTINCT c.language FROM Course c")
List<Language> findDistinctLanguages();
List<Course> findByLanguage(Language language);
@Query(value = """
SELECT c.* FROM course c
LEFT JOIN creator cr ON c.creator_id = cr.creator_id
WHERE c.ispublic = true
AND (
    LOWER(c.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.learning_object) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(cr.full_name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.language) LIKE LOWER(CONCAT('%', :searchKey, '%'))
)
""", 
countQuery = """
SELECT COUNT(c.course_id) FROM course c
LEFT JOIN creator cr ON c.creator_id = cr.creator_id
WHERE c.ispublic = true
AND (
    LOWER(c.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.learning_object) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(cr.full_name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
    OR LOWER(c.language) LIKE LOWER(CONCAT('%', :searchKey, '%'))
)
""",
nativeQuery = true)
Page<Course> searchByKey(@Param("searchKey") String searchKey, Pageable pageable);


Page<Course> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String name, String description, Pageable pageable
);



//
Long countByIsPublic(boolean isPublic);

    Long countByIsBan(boolean isBan);

    Long countByCreatedAtAfter(LocalDate date);

    Long countByCreatedAtBetween(LocalDate startDate, LocalDate endDate);

    Long countByCreatorAndIsPublic(Creator creator, boolean isPublic);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.creator.creatorId = :creatorId")
    Long countByCreatorId(@Param("creatorId") String creatorId);

    // Group by queries

        @Query("SELECT CAST(c.language AS string), COUNT(c) " +
                "FROM Course c " +
                "GROUP BY c.language")
        List<Object[]> countByLanguageGroupBy();


    @Query("SELECT CAST(c.accessMode AS string), COUNT(c) " +
            "FROM Course c " +
            "GROUP BY c.accessMode")
    List<Object[]> countByAccessModeGroupBy();

    // Top courses queries
    @Query("SELECT c FROM Course c LEFT JOIN c.enrollments e GROUP BY c ORDER BY COUNT(e) DESC")
    List<Course> findTopByEnrollments(Pageable pageable);

    @Query("SELECT c FROM Course c LEFT JOIN c.courseMetrics cm ORDER BY cm.averageRating DESC, cm.totalFeedbacks DESC")
    List<Course> findTopByRating(Pageable pageable);

    @Query("SELECT c FROM Course c LEFT JOIN c.creator cr ORDER BY cr.balance DESC")
    List<Course> findTopByRevenue(Pageable pageable);

    // Find queries
    List<Course> findByCreator(Creator creator);

    List<Course> findByIsBan(boolean isBan);

    List<Course> findByIsPublic(boolean isPublic);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.enrollments WHERE c.courseId = :courseId")
    Course findByIdWithEnrollments(@Param("courseId") Long courseId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.creator WHERE c.courseId = :courseId")
    Course findByIdWithCreator(@Param("courseId") Long courseId);

    @Query("SELECT c FROM Course c WHERE c.isPublic = true AND c.isBan = false")
    List<Course> findAllPublicAndNotBanned();

    @Query("SELECT c FROM Course c WHERE c.creator.creatorId = :creatorId AND c.isBan = false")
    List<Course> findActiveByCreatorId(@Param("creatorId") String creatorId);
}
