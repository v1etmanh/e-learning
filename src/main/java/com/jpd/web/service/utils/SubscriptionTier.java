package com.jpd.web.service.utils;

public enum SubscriptionTier {
    FREE(5, 50),        // 5 lần/ngày, 50 lần/tháng
    PREMIUM(20, 200),   // 20 lần/ngày, 200 lần/tháng
    VIP(-1, -1);        // Unlimited (-1 = không giới hạn)
    
    private final int dailyLimit;
    private final int monthlyLimit;
    
    SubscriptionTier(int dailyLimit, int monthlyLimit) {
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
    }
    
    public int getDailyLimit() {
        return dailyLimit;
    }
    
    public int getMonthlyLimit() {
        return monthlyLimit;
    }
    
    public boolean isUnlimited() {
        return dailyLimit == -1;
    }
}