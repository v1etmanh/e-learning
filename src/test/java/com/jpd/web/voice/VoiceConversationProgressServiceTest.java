package com.jpd.web.voice;

import com.jpd.web.model.VoiceConversationSession;
import com.jpd.web.repository.VoiceConversationSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceConversationProgressServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 22, 12, 0, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();

    @Mock
    private VoiceConversationSessionRepository repository;

    private VoiceConversationProgressService service;

    private MockedStatic<LocalDateTime> mockedDateTime;
    private MockedStatic<LocalDate> mockedDate;

    @BeforeEach
    void setUp() {
        service = new VoiceConversationProgressService(repository);
        mockedDateTime = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
        mockedDateTime.when(LocalDateTime::now).thenReturn(NOW);
        mockedDate = mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        mockedDate.when(LocalDate::now).thenReturn(TODAY);
    }

    @AfterEach
    void tearDown() {
        mockedDateTime.close();
        mockedDate.close();
    }

    @Test
    void start_buildsSessionWithZeroDurationAndIncomplete() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VoiceConversationSession result = service.start("user-1", "dog");

        assertThat(result.getDurationSeconds()).isZero();
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getStartedAt()).isEqualTo(NOW);
    }

    @Nested
    class Complete {

        @Test
        void throwsIllegalArgument_whenSessionNotFound() {
            when(repository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.complete("user-1", 1L));
        }

        @Test
        void idempotent_alreadyCompleted_returnsAsIs_noSave() {
            VoiceConversationSession session = VoiceConversationSession.builder()
                    .id(1L).completed(true).durationSeconds(120).build();
            when(repository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(session));

            VoiceConversationSession result = service.complete("user-1", 1L);

            assertThat(result).isSameAs(session);
            verify(repository, never()).save(any());
        }

        @Test
        void computesDuration_fromStartedAtToNow() {
            VoiceConversationSession session = VoiceConversationSession.builder()
                    .id(1L).completed(false).startedAt(NOW.minusSeconds(90)).build();
            when(repository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(session));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VoiceConversationSession result = service.complete("user-1", 1L);

            assertThat(result.getDurationSeconds()).isEqualTo(90);
            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        void clampsNegativeDuration_toZero() {
            // endedAt (NOW) before startedAt -> negative raw duration, clamped to 0
            VoiceConversationSession session = VoiceConversationSession.builder()
                    .id(1L).completed(false).startedAt(NOW.plusSeconds(90)).build();
            when(repository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(session));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VoiceConversationSession result = service.complete("user-1", 1L);

            assertThat(result.getDurationSeconds()).isZero();
        }

        @Test
        void clampsOverlongDuration_toMaxSessionSeconds() {
            VoiceConversationSession session = VoiceConversationSession.builder()
                    .id(1L).completed(false).startedAt(NOW.minusHours(5)).build();
            when(repository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(session));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VoiceConversationSession result = service.complete("user-1", 1L);

            assertThat(result.getDurationSeconds()).isEqualTo(2 * 60 * 60L);
        }
    }

    @Nested
    class Daily {

        private VoiceConversationSession qualifyingSession(String characterId, long durationSeconds) {
            return VoiceConversationSession.builder()
                    .userId("user-1").characterId(characterId)
                    .startedAt(NOW).durationSeconds(durationSeconds).completed(true).build();
        }

        @Test
        void excludesSessionsShorterThan60Seconds_fromAggregates() {
            VoiceConversationSession tooShort = qualifyingSession("dog", 45);
            VoiceConversationSession qualifying = qualifyingSession("cat", 90);
            when(repository.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndCompletedTrue(
                    "user-1", TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay()))
                    .thenReturn(List.of(tooShort, qualifying));

            VoiceDailyProgressResponse result = service.daily("user-1");

            assertThat(result.conversationCount()).isEqualTo(1);
            assertThat(result.totalSeconds()).isEqualTo(90);
        }

        @Test
        void completed_requiresBothCharacterAndTimeTargets_isAnd() {
            // Only meets the character-count target (5), not the 10-minute time target.
            List<VoiceConversationSession> sessions = List.of(
                    qualifyingSession("c1", 60), qualifyingSession("c2", 60),
                    qualifyingSession("c3", 60), qualifyingSession("c4", 60),
                    qualifyingSession("c5", 60));
            when(repository.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndCompletedTrue(
                    "user-1", TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay()))
                    .thenReturn(sessions);

            VoiceDailyProgressResponse result = service.daily("user-1");

            assertThat(result.uniqueCharactersCount()).isEqualTo(5);
            assertThat(result.totalSeconds()).isEqualTo(300); // 5 minutes, short of the 10-minute target
            assertThat(result.completed()).isFalse();
        }

        @Test
        void minutesProgress_clampedAtOneHundredPercent_whenOverTarget() {
            VoiceConversationSession longSession = qualifyingSession("dog", MINUTES_TARGET_SECONDS() * 2);
            when(repository.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndCompletedTrue(
                    "user-1", TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay()))
                    .thenReturn(List.of(longSession));

            VoiceDailyProgressResponse result = service.daily("user-1");

            assertThat(result.minutesProgress()).isEqualTo(1.0);
        }

        @Test
        void totalMinutes_isFlooredNotRounded() {
            VoiceConversationSession session = qualifyingSession("dog", 125); // 2m 5s
            when(repository.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndCompletedTrue(
                    "user-1", TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay()))
                    .thenReturn(List.of(session));

            VoiceDailyProgressResponse result = service.daily("user-1");

            assertThat(result.totalMinutes()).isEqualTo(2);
        }

        private long MINUTES_TARGET_SECONDS() {
            return VoiceConversationProgressService.MINUTES_TARGET * 60L;
        }
    }
}
