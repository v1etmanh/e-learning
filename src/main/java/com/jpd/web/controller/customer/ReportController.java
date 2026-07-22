package com.jpd.web.controller.customer;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.ReportForm;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/customer/report")
@Tag(name = "Course reports", description = "Reporting a course for inappropriate or misleading content.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class ReportController {
	@Autowired
	private ReportService reportService;

@Operation(
    summary = "Report a course",
    description = """
        Files a report against a course and increments the course's report metric.

        The reporter is taken from the `sub` claim of the bearer token. The caller must either be
        enrolled in the course or be its creator — anyone else is rejected with 401.

        The handler returns `null`, which Spring renders as **200 with an empty body**, not 201. The
        report is saved regardless.
        """)
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    required = true,
    description = "Reason, explanation and target course. Not validated — the handler does not apply `@Valid`.",
    content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = ReportForm.class),
        examples = @ExampleObject(name = "report", value = """
            {
              "type": "MISLEADING_INFORMATION",
              "detail": "Khóa học quảng cáo có 50 bài nhưng thực tế chỉ có 12 bài, nội dung không đúng mô tả.",
              "courseId": 12
            }""")))
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Report saved. Empty body.", content = @Content),
    @ApiResponse(responseCode = "401", description = "The caller is neither enrolled in the course nor its creator, or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "No course exists with this `courseId` (`code: COURSE_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Malformed or missing request body, unknown `type` value, or another unexpected server error.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping()
public ResponseEntity<?> createNewReport(@RequestBody ReportForm entity,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
	String customerId = jwt.getClaimAsString("sub");
    this.reportService.saveReport(customerId, entity);
    return null;
}

}
