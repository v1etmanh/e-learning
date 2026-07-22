package com.jpd.web.voice;

import java.time.LocalDateTime;

import com.jpd.web.model.VoiceConversationSession;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VoiceConversationSessionResponse", description = "A tracked practice conversation, before or after completion.")
public record VoiceConversationSessionResponse(
        @Schema(description = "Session identifier. Pass this to the complete endpoint.", example = "418")
        Long id,
        @Schema(description = "Character the learner practised with.", example = "tanaka-sensei")
        String characterId,
        @Schema(description = "When tracking began.", example = "2026-07-22T09:15:30.412")
        LocalDateTime startedAt,
        @Schema(description = "When the session was completed. Null while still in progress.", example = "null")
        LocalDateTime endedAt,
        @Schema(description = "How long the conversation lasted. Zero until completed.", example = "0")
        long durationSeconds,
        @Schema(description = "Whether the session has been closed off. False on start, true after completing.", example = "false")
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
