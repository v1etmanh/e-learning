package com.jpd.web.voice;

import java.time.LocalDateTime;

import com.jpd.web.model.VoiceConversationSession;

public record VoiceConversationSessionResponse(
        Long id,
        String characterId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        long durationSeconds,
        boolean completed
) {
    public static VoiceConversationSessionResponse from(VoiceConversationSession session) {
        return new VoiceConversationSessionResponse(
                session.getId(),
                session.getCharacterId(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationSeconds(),
                session.isCompleted()
        );
    }
}
