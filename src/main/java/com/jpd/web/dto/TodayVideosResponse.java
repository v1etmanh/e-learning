package com.jpd.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TodayVideosResponse", description = "The inspiration-video listing, stamped with the date it was generated.")
public class TodayVideosResponse {

    @Schema(description = "Server date the listing was generated, ISO-8601. Note this is today's date, not a filter — "
            + "the `videos` list is the whole bucket, not just today's uploads.", example = "2026-07-22")
    private String date;

    @Schema(description = "Every video in the configured R2 prefix, each with presigned URLs. Empty when the prefix holds no videos.")
    private List<InspirationVideoDTO> videos;
}
