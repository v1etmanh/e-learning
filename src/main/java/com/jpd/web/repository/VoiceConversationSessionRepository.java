package com.jpd.web.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpd.web.model.VoiceConversationSession;

public interface VoiceConversationSessionRepository extends JpaRepository<VoiceConversationSession, Long> {
    Optional<VoiceConversationSession> findByIdAndUserId(Long id, String userId);

    List<VoiceConversationSession> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanAndCompletedTrue(
            String userId, LocalDateTime from, LocalDateTime to);
}
