package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ParticipantInfo", description = "One learner taking part in a live quiz session.")
public class ParticipantInfo {
    @Schema(description = "Identifier assigned when the learner joined. Required when submitting answers.", example = "b7d1e0c4-3a92-4f55-8c10-2e6f9a0b1c33")
    private String participantId;
    @Schema(description = "Nickname the learner joined with.", example = "Thuy")
    private String name;
    @Schema(description = "PIN of the session the learner is in.", example = "482913")
    private String sessionCode;
    @Schema(description = "When the learner joined.", example = "2026-07-22T09:15:30.412")
    private LocalDateTime joinedAt;
    @Schema(description = "Running score across all questions answered so far.", example = "1750")
    private Integer currentScore;
}

