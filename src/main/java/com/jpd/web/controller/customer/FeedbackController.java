package com.jpd.web.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/customer/feedback")
@Tag(name = "Course feedback", description = "Leaving a rating and review on an enrolled course.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class FeedbackController {

    private final FeedbackService feedbackService;

    // Constructor injection thay vì field injection
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(
        summary = "Leave feedback on a course",
        description = """
            Records a star rating and review for a course the signed-in customer is enrolled in, and
            folds the rating into the course's average.

            The customer is taken from the `sub` claim of the bearer token. Three rules are enforced, in
            order: the customer must be enrolled in the course (401), they may leave feedback only once
            (400), and the review text is screened by the toxicity filter (400).

            `rate` is not range-checked — any integer is accepted and averaged into the course rating.
            On success the response is 204 with no body.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Feedback recorded. Empty body.", content = @Content),
        @ApiResponse(responseCode = "400", description = "Feedback already left for this course (`code: EXCEED_LIMIT_REQUEST`), "
                + "the review text was flagged as toxic (`code: FEEDBACK_ILLEGAL`), or `rate` was not a valid integer (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The customer is not enrolled in this course, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "`rate` or `detail` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{courseId}")
    public ResponseEntity<?> addFeedback(
            @Parameter(description = "Identifier of the enrolled course being reviewed.", required = true, example = "12")
            @PathVariable("courseId") long courseId,
            @Parameter(description = "Star rating. Intended range is 1-5, but the value is not validated.",
                    required = true, example = "5")
            @RequestParam("rate") int rate,
            @Parameter(description = "Review text. Screened by the toxicity filter before being saved.",
                    required = true, example = "Khóa học rất dễ hiểu, giảng viên phát âm chuẩn.")
            @RequestParam("detail") String detail,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String customerId = jwt.getClaimAsString("sub");
        this.feedbackService.addFeedback(customerId, courseId, detail, rate);
        return ResponseEntity.noContent().build();
    }
}