package com.jpd.web.repository;

import com.jpd.web.model.UserFeedbackUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserFeedbackUsageRepository extends JpaRepository<UserFeedbackUsage, Long> {
    
    Optional<UserFeedbackUsage> findByUserIdAndUsageDate(String userId, LocalDate usageDate);
    
    @Query("SELECT SUM(u.monthlyCount) FROM UserFeedbackUsage u WHERE u.userId = :userId " +
           "AND FUNCTION('YEAR', u.usageDate) = FUNCTION('YEAR', :date) " +
           "AND FUNCTION('MONTH', u.usageDate) = FUNCTION('MONTH', :date)")
    Integer getMonthlyUsageCount(String userId, LocalDate date);
}