package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(name = "WritingScores", description = "Gemini-generated assessment of a free-writing submission.")
public class WritingScores {
   
	 @Schema(description = "Grammar score out of 10.", example = "7.5", minimum = "0", maximum = "10")
	 private double grammar;
	    @Schema(description = "Vocabulary score out of 10.", example = "8.0", minimum = "0", maximum = "10")
    private double vocabulary;
	    @Schema(description = "Written feedback from the model. Falls back to \"Could not evaluate. Please try again.\" when the AI call or its response parsing fails.", example = "Good range of vocabulary. Watch your use of past tense in the second paragraph.")
    private String feedback;

    // getters & setters
}