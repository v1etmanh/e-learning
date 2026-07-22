package com.jpd.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.jpd.web.model.Language;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@io.swagger.v3.oas.annotations.media.Schema(name = "WritingTextEvaluateForm",
		description = "A free-writing submission to be scored by the AI. Note: the handler does not apply `@Valid`, so these constraints are not enforced.")
public class WritingTextEvaluateForm {
	@Lob
	@NotBlank
	@JsonProperty("writingText")
	@io.swagger.v3.oas.annotations.media.Schema(description = "The text to evaluate.",
			example = "Yesterday I went to the library and borrowed three books about Japanese history.")
private String writingText;
	@NotBlank
	@JsonProperty("language")
	@io.swagger.v3.oas.annotations.media.Schema(description = "Language the text is written in, passed through to the AI prompt.", example = "ENGLISH")
private String language;
}
