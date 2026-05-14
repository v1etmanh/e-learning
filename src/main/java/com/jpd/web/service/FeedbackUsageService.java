package com.jpd.web.service;

import com.jpd.web.exception.UsageLimitExceededException;
import com.jpd.web.model.UserFeedbackUsage;
import com.jpd.web.repository.UserFeedbackUsageRepository;
import com.jpd.web.service.utils.SubscriptionTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FeedbackUsageService {
    
    @Autowired
    private UserFeedbackUsageRepository usageRepository;
    
    /**
     * Kiểm tra và ghi nhận usage
     * @param userId ID người dùng
     * @param tier Gói subscription
     * @throws UsageLimitExceededException nếu vượt quá giới hạn
     */
    @Transactional
    public void checkAndRecordUsage(String userId, SubscriptionTier tier) {
        LocalDate today = LocalDate.now();
        
        // Lấy hoặc tạo mới usage record cho hôm nay
        UserFeedbackUsage usage = usageRepository
            .findByUserIdAndUsageDate(userId, today)
            .orElseGet(() -> createNewUsageRecord(userId, today, tier));
        
        // Nếu unlimited thì không cần check
        if (tier.isUnlimited()) {
            incrementUsage(usage);
            return;
        }
        
        // Kiểm tra giới hạn daily
        if (usage.getDailyCount() >= tier.getDailyLimit()) {
            throw new UsageLimitExceededException(
                "DAILY", 
                usage.getDailyCount(), 
                tier.getDailyLimit()
            );
        }
        
        // Kiểm tra giới hạn monthly
        Integer monthlyUsage = usageRepository.getMonthlyUsageCount(userId, today);
        if (monthlyUsage != null && monthlyUsage >= tier.getMonthlyLimit()) {
            throw new UsageLimitExceededException(
                "MONTHLY", 
                monthlyUsage, 
                tier.getMonthlyLimit()
            );
        }
        
        // Ghi nhận usage
        incrementUsage(usage);
    }
    
    /**
     * Lấy thông tin usage của user
     */
    public Map<String, Object> getUserUsageInfo(String userId, SubscriptionTier tier) {
        LocalDate today = LocalDate.now();
        
        UserFeedbackUsage todayUsage = usageRepository
            .findByUserIdAndUsageDate(userId, today)
            .orElse(null);
        
        Integer monthlyUsage = usageRepository.getMonthlyUsageCount(userId, today);
        
        Map<String, Object> info = new HashMap<>();
        info.put("tier", tier.name());
        info.put("dailyUsed", todayUsage != null ? todayUsage.getDailyCount() : 0);
        info.put("dailyLimit", tier.getDailyLimit());
        info.put("dailyRemaining", tier.isUnlimited() ? -1 : 
            tier.getDailyLimit() - (todayUsage != null ? todayUsage.getDailyCount() : 0));
        
        info.put("monthlyUsed", monthlyUsage != null ? monthlyUsage : 0);
        info.put("monthlyLimit", tier.getMonthlyLimit());
        info.put("monthlyRemaining", tier.isUnlimited() ? -1 : 
            tier.getMonthlyLimit() - (monthlyUsage != null ? monthlyUsage : 0));
        
        info.put("isUnlimited", tier.isUnlimited());
        info.put("lastUsedAt", todayUsage != null ? todayUsage.getLastUsedAt() : null);
        
        return info;
    }
    
    /**
     * Kiểm tra xem user có thể sử dụng không (không throw exception)
     */
    public boolean canUse(String userId, SubscriptionTier tier) {
        try {
            LocalDate today = LocalDate.now();
            
            if (tier.isUnlimited()) {
                return true;
            }
            
            UserFeedbackUsage usage = usageRepository
                .findByUserIdAndUsageDate(userId, today)
                .orElse(null);
            
            // Check daily
            if (usage != null && usage.getDailyCount() >= tier.getDailyLimit()) {
                return false;
            }
            
            // Check monthly
            Integer monthlyUsage = usageRepository.getMonthlyUsageCount(userId, today);
            if (monthlyUsage != null && monthlyUsage >= tier.getMonthlyLimit()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private UserFeedbackUsage createNewUsageRecord(String userId, LocalDate date, SubscriptionTier tier) {
        UserFeedbackUsage usage = new UserFeedbackUsage();
        usage.setUserId(userId);
        usage.setUsageDate(date);
        usage.setDailyCount(0);
        usage.setMonthlyCount(0);
        usage.setSubscriptionTier(tier.name());
        return usage;
    }
    
    private void incrementUsage(UserFeedbackUsage usage) {
        usage.setDailyCount(usage.getDailyCount() + 1);
        usage.setMonthlyCount(usage.getMonthlyCount() + 1);
        usage.setLastUsedAt(LocalDateTime.now());
        usageRepository.save(usage);
    }
}