package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.jpd.web.model.TypeOfContent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "RejectedContent", description = "One content item that content moderation refused to store.")
public  class RejectedContent {
    @Schema(description = "Identifier of the rejected item, when it already had one.", example = "9042")
    private Long mcId;
    @Schema(description = "The text that was rejected.", example = "...")
    private String content;
    @Schema(description = "Why the moderation service rejected it.", example = "HATE_SPEECH")
    private String reason; // Từ moderation API // Từ moderation API
    @Schema(description = "Kind of content the item was.", example = "MULTIPLE_CHOICE")
    private TypeOfContent type;
}
