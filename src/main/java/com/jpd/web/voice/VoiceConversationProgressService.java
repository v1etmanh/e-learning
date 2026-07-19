package com.jpd.web.voice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpd.web.model.VoiceConversationSession;
import com.jpd.web.repository.VoiceConversationSessionRepository;

@Service
public class VoiceConversationProgressService {
    public static final int CHARACTERS_TARGET = 5;
    public static final int MINUTES_TARGET = 10;
    private static final long MIN_QUALIFYING_SESSION_SECONDS = 60L;
    private static final long MAX_SESSION_SECONDS = 2 * 60 * 60L;

    private final VoiceConversationSessionRepository repository;

    public VoiceConversationProgressService(VoiceConversationSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VoiceConversationSession start(String userId, String characterId) {
        VoiceConversationSession session = VoiceConversationSession.builder()
                .userId(userId)
                .characterId(characterId)
                .startedAt(LocalDateTime.now())
                .durationSeconds(0)
                .completed(false)
                .build();
        return repository.save(session);
    }

    @Transactional
    public VoiceConversationSession complete(String userId, Long sessionId) {
        VoiceConversationSession session = repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Voice conversation session not found"));
        if (session.isCompleted()) {
            return session;
        }

        LocalDateTime endedAt = LocalDateTime.now();
        long duration = java.time.Duration.between(session.getStartedAt(), endedAt).getSeconds();
        session.setEndedAt(endedAt);
        session.setDurationSeconds(Math.min(Math.max(duration, 0), MAX_SESSION_SECONDS));
        session.setCompleted(true);
        return repository.save(session);
    }

    @Transactional(readOnly = true)
    public VoiceDailyProgressResponse daily(String userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        List<VoiceConversationSession> sessions = repository
                .findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndCompletedTrue(userId, from, to)
                .stream()
                .filter(session -> session.getDurationSeconds() >= MIN_QUALIFYING_SESSION_SECONDS)
                .toList();

        Set<String> characters = sessions.stream()
                .map(VoiceConversationSession::getCharacterId)
                .collect(Collectors.toSet());
        long totalSeconds = sessions.stream()
                .mapToLong(VoiceConversationSession::getDurationSeconds)
                .sum();
        int totalMinutes = (int) Math.floor(totalSeconds / 60.0);
        double charactersProgress = Math.min(1.0, characters.size() / (double) CHARACTERS_TARGET);
        double minutesProgress = Math.min(1.0, totalSeconds / (MINUTES_TARGET * 60.0));

        return new VoiceDailyProgressResponse(
                today,
                sessions.size(),
                characters.size(),
                totalSeconds,
                totalMinutes,
                CHARACTERS_TARGET,
                MINUTES_TARGET,
                charactersProgress,
                minutesProgress,
                characters.size() >= CHARACTERS_TARGET && totalSeconds >= MINUTES_TARGET * 60L
        );
    }
}
