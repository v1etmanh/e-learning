package com.jpd.web.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "voice_conversation_session", indexes = {
        @Index(name = "idx_voice_session_user_started", columnList = "user_id, started_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceConversationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "character_id", nullable = false, length = 80)
    private String characterId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(nullable = false)
    private boolean completed;
}
