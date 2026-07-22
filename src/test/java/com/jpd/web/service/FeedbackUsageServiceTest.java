package com.jpd.web.service;

import com.jpd.web.exception.UsageLimitExceededException;
import com.jpd.web.model.UserFeedbackUsage;
import com.jpd.web.repository.UserFeedbackUsageRepository;
import com.jpd.web.service.utils.SubscriptionTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackUsageServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);

    @Mock
    private UserFeedbackUsageRepository usageRepository;

    @InjectMocks
    private FeedbackUsageService feedbackUsageService;

    private MockedStatic<LocalDate> mockedLocalDate;

    @BeforeEach
    void pinToday() {
        mockedLocalDate = mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        mockedLocalDate.when(LocalDate::now).thenReturn(TODAY);
    }

    @AfterEach
    void unpinToday() {
        mockedLocalDate.close();
    }

    private UserFeedbackUsage usageWith(int dailyCount) {
        UserFeedbackUsage usage = new UserFeedbackUsage();
        usage.setUserId("user-1");
        usage.setUsageDate(TODAY);
        usage.setDailyCount(dailyCount);
        usage.setMonthlyCount(dailyCount);
        return usage;
    }

    @Nested
    class CheckAndRecordUsage {

        @Test
        void createsNewRecord_whenNoUsageToday() {
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.empty());
            when(usageRepository.getMonthlyUsageCount("user-1", TODAY)).thenReturn(0);

            feedbackUsageService.checkAndRecordUsage("user-1", SubscriptionTier.FREE);

            ArgumentCaptor<UserFeedbackUsage> captor = ArgumentCaptor.forClass(UserFeedbackUsage.class);
            verify(usageRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
            assertThat(captor.getValue().getDailyCount()).isEqualTo(1);
        }

        @Test
        void vipTier_skipsLimitChecks_alwaysIncrements() {
            UserFeedbackUsage usage = usageWith(999);
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.of(usage));

            feedbackUsageService.checkAndRecordUsage("user-1", SubscriptionTier.VIP);

            verify(usageRepository, org.mockito.Mockito.never()).getMonthlyUsageCount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            verify(usageRepository).save(usage);
            assertThat(usage.getDailyCount()).isEqualTo(1000);
        }

        @Test
        void throwsDailyLimitExceeded_whenDailyCountAtLimit() {
            UserFeedbackUsage usage = usageWith(SubscriptionTier.FREE.getDailyLimit());
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.of(usage));

            UsageLimitExceededException ex = assertThrows(UsageLimitExceededException.class,
                    () -> feedbackUsageService.checkAndRecordUsage("user-1", SubscriptionTier.FREE));

            assertThat(ex.getLimitType()).isEqualTo("DAILY");
        }

        @Test
        void throwsMonthlyLimitExceeded_whenDailyOkButMonthlyAtLimit() {
            UserFeedbackUsage usage = usageWith(0);
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.of(usage));
            when(usageRepository.getMonthlyUsageCount("user-1", TODAY))
                    .thenReturn(SubscriptionTier.FREE.getMonthlyLimit());

            UsageLimitExceededException ex = assertThrows(UsageLimitExceededException.class,
                    () -> feedbackUsageService.checkAndRecordUsage("user-1", SubscriptionTier.FREE));

            assertThat(ex.getLimitType()).isEqualTo("MONTHLY");
        }

        @Test
        void incrementsBothCounters_onHappyPath() {
            UserFeedbackUsage usage = usageWith(1);
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.of(usage));
            when(usageRepository.getMonthlyUsageCount("user-1", TODAY)).thenReturn(1);

            feedbackUsageService.checkAndRecordUsage("user-1", SubscriptionTier.FREE);

            assertThat(usage.getDailyCount()).isEqualTo(2);
            assertThat(usage.getMonthlyCount()).isEqualTo(2);
            assertThat(usage.getLastUsedAt()).isNotNull();
        }
    }

    @Nested
    class GetUserUsageInfo {

        @Test
        void returnsZeroUsed_whenNoRowToday() {
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.empty());
            when(usageRepository.getMonthlyUsageCount("user-1", TODAY)).thenReturn(null);

            Map<String, Object> info = feedbackUsageService.getUserUsageInfo("user-1", SubscriptionTier.FREE);

            assertThat(info.get("dailyUsed")).isEqualTo(0);
            assertThat(info.get("monthlyUsed")).isEqualTo(0);
        }

        @Test
        void vipTier_returnsSentinelMinusOneForRemaining() {
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.empty());
            when(usageRepository.getMonthlyUsageCount("user-1", TODAY)).thenReturn(null);

            Map<String, Object> info = feedbackUsageService.getUserUsageInfo("user-1", SubscriptionTier.VIP);

            assertThat(info.get("dailyRemaining")).isEqualTo(-1);
            assertThat(info.get("monthlyRemaining")).isEqualTo(-1);
        }
    }

    @Nested
    class CanUse {

        @Test
        void returnsFalse_whenDailyLimitReached() {
            UserFeedbackUsage usage = usageWith(SubscriptionTier.FREE.getDailyLimit());
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.of(usage));

            assertThat(feedbackUsageService.canUse("user-1", SubscriptionTier.FREE)).isFalse();
        }

        @Test
        void returnsTrue_whenUnderLimits() {
            UserFeedbackUsage usage = usageWith(0);
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY)).thenReturn(Optional.of(usage));
            when(usageRepository.getMonthlyUsageCount("user-1", TODAY)).thenReturn(0);

            assertThat(feedbackUsageService.canUse("user-1", SubscriptionTier.FREE)).isTrue();
        }

        @Test
        void swallowsException_returnsFalse_asFailSafe() {
            when(usageRepository.findByUserIdAndUsageDate("user-1", TODAY))
                    .thenThrow(new RuntimeException("db down"));

            assertThat(feedbackUsageService.canUse("user-1", SubscriptionTier.FREE)).isFalse();
        }
    }
}
