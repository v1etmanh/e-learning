package com.jpd.web.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FeedbackSimpleDto", description = "One review left by a learner on a course.")
public class FeedbackSimpleDto {

    @Schema(description = "Feedback identifier.", example = "884")
    private Long feedbackId;

    @Schema(description = "Review text written by the learner.", example = "Khóa học rất dễ hiểu, giảng viên phát âm chuẩn.")
    private String content;

    @Schema(description = "Star rating the learner gave the course.", example = "5", minimum = "1", maximum = "5")
    private int rate;

    @Schema(description = "Date the feedback was submitted.", example = "2026-05-14")
    private LocalDate createDate;

    @Schema(description = "Keycloak user id of the learner who left the feedback.", example = "9c2e5f77-11ab-42d0-8b3e-6ce0d4a9e777")
    private String  customerId;
}