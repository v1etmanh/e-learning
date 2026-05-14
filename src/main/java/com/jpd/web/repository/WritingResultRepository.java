package com.jpd.web.repository;

import com.jpd.web.model.WritingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface WritingResultRepository 
        extends JpaRepository<WritingResult, Long> {
    @Query("SELECT COUNT(w) FROM WritingResult w WHERE w.customerId = :customerId " +
            "AND w.createDate >= :startOfDay AND w.createDate <= :endOfDay")
    Long countTodayByCustomerId(
            @Param("customerId") String customerId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
