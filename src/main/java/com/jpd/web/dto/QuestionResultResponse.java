package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "QuestionResultResponse", description = "Results for every participant once the host closes a question.")
public class QuestionResultResponse {
    @Schema(description = "Identifier of the question that just closed.", example = "9042")
    private Long questionId;
    @Schema(description = "Per-participant outcome for this question.")
    private List<QuestionResult> results;
    @Schema(description = "The correct answer, revealed now that the question is closed.", example = "B")
    private String correctAnswer;
}
