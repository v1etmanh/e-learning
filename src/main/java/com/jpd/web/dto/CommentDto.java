package com.jpd.web.dto;

import java.sql.Date;
import java.util.List;

import com.jpd.web.model.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(name = "CommentDto", description = "A comment on a course.")
public class CommentDto {

@io.swagger.v3.oas.annotations.media.Schema(description = "The comment text.", example = "Bài 12 giải thích rất rõ, cảm ơn thầy!")
private String comment;

@io.swagger.v3.oas.annotations.media.Schema(description = """
		Author of the comment. Only meaningful on create and update, where it is the caller's Keycloak
		user id. When listing a course's comments this is always the literal string `Anonymous`, because
		the `Comment` entity does not store a reference to its author.
		""", example = "Anonymous")
private String createBy;
}
