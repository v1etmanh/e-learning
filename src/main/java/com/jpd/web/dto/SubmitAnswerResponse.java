package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "SubmitAnswerResponse", description = "Acknowledgement of a submitted answer. Whether it was correct is not revealed here - that comes when the host ends the question.")
public class SubmitAnswerResponse {
    @Schema(description = "Whether the answer was accepted.", example = "true")
    private boolean success;
    @Schema(description = "Human-readable outcome.", example = "Answer submitted")
    private String message;
    @Schema(description = "How many participants have answered the current question so far.", example = "18")
    private int totalAnswered;
    @Schema(description = "How many participants are in the session.", example = "24")
    private int totalParticipants;
}