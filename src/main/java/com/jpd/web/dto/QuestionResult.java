package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "QuestionResult", description = "How one participant did on one question.")
public class QuestionResult {
    @Schema(description = "Identifier of the participant.", example = "b7d1e0c4-3a92-4f55-8c10-2e6f9a0b1c33")
    private String participantId;
    @Schema(description = "Nickname of the participant.", example = "Thuy")
    private String participantName;
    @Schema(description = "What the participant answered.", example = "B")
    private String answer;
    @Schema(description = "Whether the answer was correct.", example = "true")
    private boolean correct;
    @Schema(description = "Points earned on this question. Faster correct answers score higher.", example = "850")
    private int points;
    @Schema(description = "The participant's running total after this question.", example = "1750")
    private int totalScore;
}