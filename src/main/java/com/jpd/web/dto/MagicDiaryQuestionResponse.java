package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagicDiaryQuestionResponse {
    private String status;
    private String answer;
    private String correction;
    private String suggestedQuestion;
}
