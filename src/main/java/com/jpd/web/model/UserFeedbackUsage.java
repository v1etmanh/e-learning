package com.jpd.web.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_feedback_usage")
public class UserFeedbackUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private LocalDate usageDate; // Ngày sử dụng
    
    @Column(nullable = false)
    private Integer dailyCount = 0; // Số lần dùng trong ngày
    
    @Column(nullable = false)
    private Integer monthlyCount = 0; // Số lần dùng trong tháng
    
    @Column
    private LocalDateTime lastUsedAt; // Lần cuối sử dụng
    
    @Column
    private String subscriptionTier; // FREE, PREMIUM, VIP
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDate getUsageDate() {
        return usageDate;
    }
    
    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }
    
    public Integer getDailyCount() {
        return dailyCount;
    }
    
    public void setDailyCount(Integer dailyCount) {
        this.dailyCount = dailyCount;
    }
    
    public Integer getMonthlyCount() {
        return monthlyCount;
    }
    
    public void setMonthlyCount(Integer monthlyCount) {
        this.monthlyCount = monthlyCount;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public String getSubscriptionTier() {
        return subscriptionTier;
    }
    
    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }
}