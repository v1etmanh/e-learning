package com.jpd.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IeltsBrainstormForm {
    @NotBlank(message = "Prompt is required")
    private String prompt;
}
