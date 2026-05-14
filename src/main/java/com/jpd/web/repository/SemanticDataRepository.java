package com.jpd.web.repository;

import com.jpd.web.model.SemanticData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SemanticDataRepository extends JpaRepository<SemanticData, Long> {
    @Query("SELECT COUNT(s) FROM SemanticData s WHERE s.customerId = :customerId " +
            "AND s.createDate >= :startOfDay AND s.createDate <= :endOfDay")
    Long countTodayByCustomerId(
            @Param("customerId") String customerId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
    List<SemanticData> findByCustomerId(String customerId);

    List<SemanticData> findByModuleIdAndCustomerId(Long moduleId, String customerId);

}
