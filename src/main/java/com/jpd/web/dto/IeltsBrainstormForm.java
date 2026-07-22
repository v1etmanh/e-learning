package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IeltsBrainstormForm", description = "An IELTS essay prompt to brainstorm ideas for.")
public class IeltsBrainstormForm {
    @NotBlank(message = "Prompt is required")
    @Schema(description = "The prompt sent verbatim to Gemini. Nothing is prepended or templated around it.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Some people believe that university education should be free for everyone. To what extent do you agree or disagree?")
    private String prompt;
}
