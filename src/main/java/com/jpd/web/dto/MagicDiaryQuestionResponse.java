package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MagicDiaryQuestionResponse", description = "The AI's answer to a Magic Diary question. "
		+ "Failures are reported in `status`, not as an HTTP error — the endpoint still returns 200.")
public class MagicDiaryQuestionResponse {

    @Schema(description = "Outcome of the request. `OUT_OF_SCOPE` is used when the question cannot be answered from "
            + "`referenceNotes` and is also the default if the model omits a status. `ERROR` means the AI call or "
            + "its response parsing failed.",
            example = "ANSWERED", allowableValues = {"ANSWERED", "OUT_OF_SCOPE", "ERROR"})
    private String status;

    @Schema(description = "The answer, drawn only from the supplied reference notes. Empty string when there is none.",
            example = "Mạc phủ Tokugawa áp dụng sakoku để hạn chế ảnh hưởng của phương Tây và giữ ổn định chính trị trong nước.")
    private String answer;

    @Schema(description = "Correction of a mistaken assumption in the question. Empty string when there is nothing to correct.", example = "")
    private String correction;

    @Schema(description = "A follow-up question the learner could ask next. Empty string when none is offered.",
            example = "Chính sách sakoku kết thúc như thế nào?")
    private String suggestedQuestion;
}
