package com.jpd.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagicDiaryEvaluateForm {
    @NotBlank
    private String writingText;

    @NotBlank
    private String language;

    @NotBlank
    private String lessonTitle;

    @NotBlank
    private String referenceNotes;
}
