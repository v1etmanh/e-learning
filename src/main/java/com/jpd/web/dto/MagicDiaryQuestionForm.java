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
@Schema(name = "MagicDiaryQuestionForm", description = "A learner's question about the lesson they are journalling on. "
		+ "The AI answers only from `referenceNotes`; anything outside that scope comes back as `OUT_OF_SCOPE`.")
public class MagicDiaryQuestionForm {
    @NotBlank
    @Schema(description = "The learner's question.", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Tại sao Mạc phủ Tokugawa lại thực hiện chính sách bế quan tỏa cảng?")
    private String question;

    @NotBlank
    @Schema(description = "Title of the lesson the question is about.", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Thời kỳ Edo và Mạc phủ Tokugawa")
    private String lessonTitle;

    @NotBlank
    @Schema(description = "Key facts from the lesson. The model may only answer from these.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Mạc phủ Tokugawa: 1603-1868. Thủ đô Edo (Tokyo ngày nay). Chính sách bế quan tỏa cảng sakoku.")
    private String referenceNotes;
}
