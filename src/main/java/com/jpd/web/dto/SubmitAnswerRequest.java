package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Language;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Builder
@Schema(name = "SubmitAnswerRequest", description = "A learner's answer to the question currently open in a session.")
public class SubmitAnswerRequest {
    @Schema(description = "PIN of the session.", example = "482913")
    private String sessionCode;
    @Schema(description = "Identifier the learner received when joining.", example = "b7d1e0c4-3a92-4f55-8c10-2e6f9a0b1c33")
    private String participantId;
    @Schema(description = "Identifier of the question being answered.", example = "9042")
    private Long questionId;
    @Schema(description = "The learner's answer: the chosen option for a multiple-choice question, or the typed text for a gap-fill.", example = "B")
    private String answer;
}