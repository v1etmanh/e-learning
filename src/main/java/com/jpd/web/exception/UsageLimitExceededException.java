package com.jpd.web.exception;

public class UsageLimitExceededException extends RuntimeException {
    private String limitType; // "DAILY" or "MONTHLY"
    private int currentUsage;
    private int limit;
    
    public UsageLimitExceededException(String limitType, int currentUsage, int limit) {
        super(String.format("Đã vượt quá giới hạn %s. Đã dùng: %d/%d", 
            limitType.toLowerCase(), currentUsage, limit));
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }
    
    public String getLimitType() {
        return limitType;
    }
    
    public int getCurrentUsage() {
        return currentUsage;
    }
    
    public int getLimit() {
        return limit;
    }
}