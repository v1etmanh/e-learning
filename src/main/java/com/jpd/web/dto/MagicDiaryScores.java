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
@Schema(name = "MagicDiaryScores", description = """
        Grading of a Magic Diary entry.

        If the AI call or its response parsing fails, a neutral fallback is returned instead of an error:
        all three scores are `5.0` and `feedback` reads "Could not evaluate. Please try again."
        """)
public class MagicDiaryScores {

    @Schema(description = "How well the entry matches the facts in `referenceNotes`, out of 10.", example = "8.5", minimum = "0", maximum = "10")
    private double contentAccuracy;

    @Schema(description = "Grammar score out of 10.", example = "7.0", minimum = "0", maximum = "10")
    private double grammar;

    @Schema(description = "Vocabulary score out of 10.", example = "7.5", minimum = "0", maximum = "10")
    private double vocabulary;

    @Schema(description = "Written feedback on the entry.",
            example = "Bạn nắm đúng mốc thời gian 1603-1868. Câu thứ hai nên dùng thể quá khứ.")
    private String feedback;

    @Schema(description = "Concrete suggestion for what to practise next.",
            example = "Viết thêm một đoạn về chính sách sakoku và ảnh hưởng của nó.")
    private String nextStep;
}
