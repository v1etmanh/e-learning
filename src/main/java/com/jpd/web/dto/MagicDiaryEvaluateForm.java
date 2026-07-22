package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MagicDiaryEvaluateForm", description = "A Magic Diary entry to be graded against the lesson the learner just studied.")
public class MagicDiaryEvaluateForm {
    @NotBlank
    @Schema(description = "What the learner wrote about the lesson.", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Hôm nay tôi học về thời Edo. Mạc phủ Tokugawa cai trị Nhật Bản trong hơn 250 năm.")
    private String writingText;

    @NotBlank
    @Schema(description = "Language the entry is written in.", requiredMode = Schema.RequiredMode.REQUIRED, example = "VIETNAMESE")
    private String language;

    @NotBlank
    @Schema(description = "Title of the lesson the entry is about. Gives the model the topic to grade content accuracy against.",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "Thời kỳ Edo và Mạc phủ Tokugawa")
    private String lessonTitle;

    @NotBlank
    @Schema(description = "Key facts from the lesson. The model checks the entry against these when scoring `contentAccuracy`.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Mạc phủ Tokugawa: 1603-1868. Thủ đô Edo (Tokyo ngày nay). Chính sách bế quan tỏa cảng sakoku.")
    private String referenceNotes;
}
