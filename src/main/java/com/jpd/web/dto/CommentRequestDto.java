package com.jpd.web.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CommentRequestDto", description = "Body for creating or updating a course comment.")
public class CommentRequestDto {

    @NotBlank(message = "Comment content cannot be empty")
    @Size(min = 1, max = 1000, message = "Comment must be between 1 and 1000 characters")
    @Schema(description = "The comment text. On create it is screened by the toxicity filter; on update it is not.",
            requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 1000,
            example = "Bài 12 giải thích rất rõ, cảm ơn thầy!")
    private String content;
}