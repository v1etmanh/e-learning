package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InspirationVideoDTO", description = "One inspiration video stored in Cloudflare R2, with time-limited presigned playback URLs.")
public class InspirationVideoDTO {

    @Schema(description = "The R2 object key, used as the video identifier.", example = "inspiration/2026/never-give-up.mp4")
    private String id;

    @Schema(description = "Title derived from the object key.", example = "Never Give Up")
    private String title;

    @Schema(description = "Short description of the video.", example = "Inspiration video")
    private String description;

    @Schema(description = "Speaker's name. Empty string in the listing.", example = "")
    private String speaker;

    @Schema(description = "Topic label.", example = "motivation")
    private String topic;

    @Schema(description = "Presigned R2 URL for playback. Expires after the configured window — re-fetch the listing once it does.",
            example = "https://r2.example.com/inspiration/2026/never-give-up.mp4?X-Amz-Signature=abc123&X-Amz-Expires=3600")
    private String videoUrl;        // presigned URL (có thời hạn)

    @Schema(description = "Presigned R2 URL for the thumbnail image.",
            example = "https://r2.example.com/inspiration/2026/never-give-up.jpg?X-Amz-Signature=def456&X-Amz-Expires=3600")
    private String thumbnailUrl;    // presigned URL cho thumbnail

    @Schema(description = "English transcript. Null in the listing response.", example = "null")
    private String transcript;      // transcript tiếng Anh (chỉ có trong detail, list trả null)

    @Schema(description = "Video length in seconds.", example = "412")
    private Integer durationSeconds;

    @Schema(description = "Difficulty of the spoken English in the video.", example = "INTERMEDIATE")
    private String difficultyLevel;
}
