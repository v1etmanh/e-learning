package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

@io.swagger.v3.oas.annotations.media.Schema(name = "SemanticResult",
        description = "Result of comparing a learner's transcribed speech against the expected sentence.")
public class SemanticResult {

    @JsonProperty("match")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Whether the spoken answer was judged to match the expected sentence.", example = "true")
    private boolean match;

    @JsonProperty("similarity_score")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Semantic similarity between what was said and what was expected.",
            example = "0.92", minimum = "0", maximum = "1")
    private double similarityScore;

    @JsonProperty("user_answer")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Transcription of the uploaded audio.", example = "今日はいい天気ですね")
    private String userAnswer;

    @JsonProperty("expected_answer")
    @io.swagger.v3.oas.annotations.media.Schema(description = "The sentence the learner was asked to say, echoed back from the request.",
            example = "今日はいい天気ですね")
    private String expectedAnswer;

    @JsonProperty("feedback")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Pronunciation and phrasing feedback.",
            example = "Phát âm rõ ràng. Chú ý kéo dài âm ở cuối câu khi dùng ね.")
    private String feedback;

    @JsonProperty("has_error")
    @io.swagger.v3.oas.annotations.media.Schema(description = "True when the evaluation pipeline itself failed rather than the learner being wrong.", example = "false")
    private boolean hasError;

    @JsonProperty("error_message")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Details of the pipeline failure. Null when `has_error` is false.", example = "null")
    private String errorMessage;
}
