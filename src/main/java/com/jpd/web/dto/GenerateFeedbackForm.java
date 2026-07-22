package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Schema(name = "GenerateFeedbackForm", description = "A question and a learner answer for the AI to write feedback on.")
public class GenerateFeedbackForm {
@Schema(description = "The answer to critique.", example = "I go to school yesterday with my friend.")
private String answer;
@Schema(description = "The question the answer responds to.", example = "Describe what you did yesterday.")
private String question;
}
