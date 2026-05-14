package com.jpd.web.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Status;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface CreatorRepository extends JpaRepository<Creator, String> {


       @Query("""
    SELECT COUNT(DISTINCT e)
    FROM Enrollment e
    JOIN e.course c
    WHERE c.creator.creatorId = :creatorId
""")
       int countTotalStudentsByCreatorId(@Param("creatorId") String creatorId);


       @Query("""
    SELECT AVG(f.averageRating)
    FROM CourseMetrics f
    JOIN f.course t
    WHERE t.creator.creatorId = :creatorId
""")
       Double getAverageRatingByCreatorId(@Param("creatorId") String creatorId);


       // Admin management queries
       List<Creator> findAllByStatus(Status status);

       List<Creator> findByFullNameContainingIgnoreCase(String fullName);

       // Count queries
       Long countByBan(boolean ban);

       Long countByStatus(Status status);

       Long countByCreateDateAfter(Date date);

       Long countByCreateDateBetween(Date startDate, Date endDate);

       Long countByCreateDateEquals(Date date);

       Long countByBalanceGreaterThan(Double balance);

       @Query("SELECT COUNT(c) FROM Creator c WHERE SIZE(c.courses) = 0")
       Long countCreatorsWithoutCourses();

       // Sum and aggregate queries
       @Query("SELECT COALESCE(SUM(c.balance), 0.0) FROM Creator c")
       Double sumAllBalances();

       @Query("SELECT MAX(c.balance) FROM Creator c")
       Double findMaxBalance();

       // Find queries
       List<Creator> findTopByOrderByBalanceDesc(Pageable pageable);

       List<Creator> findByBan(boolean ban);

       List<Creator> findByStatus(Status status);

       @Query("SELECT c FROM Creator c WHERE c.warningCount >= :threshold")
       List<Creator> findCreatorsWithWarningsAboveThreshold(@Param("threshold") Integer threshold);

       @Query("SELECT c FROM Creator c LEFT JOIN FETCH c.courses WHERE c.creatorId = :creatorId")
       Creator findByIdWithCourses(@Param("creatorId") String creatorId);

       @Query("SELECT c FROM Creator c WHERE c.reputationScore < :minScore")
       List<Creator> findCreatorsWithLowReputation(@Param("minScore") Integer minScore);
}
