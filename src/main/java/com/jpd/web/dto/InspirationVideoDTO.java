package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspirationVideoDTO {

    private String id;
    private String title;
    private String description;
    private String speaker;
    private String topic;
    private String videoUrl;        // presigned URL (có thời hạn)
    private String thumbnailUrl;    // presigned URL cho thumbnail
    private String transcript;      // transcript tiếng Anh (chỉ có trong detail, list trả null)
    private Integer durationSeconds;
    private String difficultyLevel;
}
