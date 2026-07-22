package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Schema(name = "KahootDto", description = "Summary of a Kahoot quiz owned by a creator.")
public class KahootDto {
@Schema(description = "Quiz title.", example = "On tap Kanji N5")
private String title;
@Schema(description = "When the quiz was created.", example = "2026-07-01T14:22:10.000")
private LocalDateTime createDate;
@Schema(description = "How many questions the quiz holds.", example = "15")
private int numberQuestion;
@Schema(description = "Quiz identifier. Pass this as `kahootId` when opening a live session.", example = "77")
private long id;
}
