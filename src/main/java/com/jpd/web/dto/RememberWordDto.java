package com.jpd.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.jpd.web.model.Language;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@io.swagger.v3.oas.annotations.media.Schema(name = "RememberWordDto", description = """
		A vocabulary entry in the learner's personal dictionary.

		Note: the handlers do not apply `@Valid`, so the constraints below are not enforced. On create the
		server echoes the submitted body back unchanged — `rwId` in the response is whatever was sent, not
		the generated id. Re-read the dictionary to get real ids.
		""")
public class RememberWordDto {

	@io.swagger.v3.oas.annotations.media.Schema(description = "Entry identifier. Required on update and delete; ignored on create.", example = "204")
	private long rwId;

	@NotBlank
	@io.swagger.v3.oas.annotations.media.Schema(description = "The word or phrase being learned.", example = "天気")
	private String word;

	@NotBlank
	@io.swagger.v3.oas.annotations.media.Schema(description = "Meaning of the word in the learner's own language.", example = "thời tiết")
	private String meaning;

	@NotBlank
	@io.swagger.v3.oas.annotations.media.Schema(description = "Extra notes: reading, nuance, when to use it.",
			example = "Đọc là てんき. Dùng trong hội thoại hằng ngày về thời tiết.")
	private String description;

	@NotEmpty
	@io.swagger.v3.oas.annotations.media.Schema(description = "Words with a similar meaning.", example = "[\"気候\", \"天候\"]")
	private List<String> synonyms;

	@NotEmpty
	@io.swagger.v3.oas.annotations.media.Schema(description = "Example sentences using the word.", example = "[\"今日はいい天気ですね\"]")
	private List<String> example;

	@io.swagger.v3.oas.annotations.media.Schema(description = "Language the word belongs to.", example = "JAPANESE")
	private Language language;

	@io.swagger.v3.oas.annotations.media.Schema(description = "Number of votes the entry has received. Preserved across updates — a value sent in an update body is ignored.",
			example = "3")
	private Integer voteCount; // Số lượng vote

	@io.swagger.v3.oas.annotations.media.Schema(description = "Email of the learner who created the entry.", example = "nguyenthuy010605@gmail.com")
	private String customerEmail; // Email của người tạo từ
}
