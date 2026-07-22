package com.jpd.web.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.TodayVideosResponse;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.InspirationVideoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inspiration-videos")
@Slf4j
@Tag(name = "Inspiration videos", description = "Motivational listening material served from Cloudflare R2 with presigned URLs.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class InspirationVideoController {

    private final InspirationVideoService inspirationVideoService;

    public InspirationVideoController(InspirationVideoService inspirationVideoService) {
        this.inspirationVideoService = inspirationVideoService;
    }

    /**
     * GET /api/inspiration-videos/today
     * Trả về TẤT CẢ video từ R2 bucket kèm presigned URL
     */
    @Operation(
        summary = "List inspiration videos",
        description = """
            Lists every video under the configured R2 prefix, each with freshly generated presigned URLs
            for the video and its thumbnail. The URLs expire after the configured window, so re-fetch
            this listing rather than caching them.

            Despite the `/today` path and the `date` field in the response, this is **not** filtered by
            date — `date` is simply the server's current date and the list is the whole prefix. Takes no
            parameters and is the same for every caller.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All available inspiration videos.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TodayVideosResponse.class),
                examples = @ExampleObject(name = "videos", value = """
                    {
                      "date": "2026-07-22",
                      "videos": [
                        {
                          "id": "inspiration/2026/never-give-up.mp4",
                          "title": "Never Give Up",
                          "description": "Inspiration video",
                          "speaker": "",
                          "topic": "motivation",
                          "videoUrl": "https://r2.example.com/inspiration/2026/never-give-up.mp4?X-Amz-Signature=abc123&X-Amz-Expires=3600",
                          "thumbnailUrl": "https://r2.example.com/inspiration/2026/never-give-up.jpg?X-Amz-Signature=def456&X-Amz-Expires=3600",
                          "transcript": null,
                          "durationSeconds": 412,
                          "difficultyLevel": "INTERMEDIATE"
                        }
                      ]
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "R2 could not be listed or the presigned URLs could not be generated.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/today")
    public ResponseEntity<TodayVideosResponse> getAllVideos() {
        TodayVideosResponse response = inspirationVideoService.getAllVideos();
        return ResponseEntity.ok(response);
    }
}
