package com.jpd.web.controller.creator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.Enrollment;
import com.jpd.web.service.EnrollmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("api/creator/enrollment")
@Tag(name = "Creator enrollments", description = "Who has enrolled in the signed-in creator's courses.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class EnrollmentCreatorController {
@Autowired
private EnrollmentService enrollmentService;

@Operation(
    summary = "List a course's enrollments",
    description = """
        Returns every enrollment on a course the signed-in creator owns, each with the learner's feedback
        eagerly loaded — the roster behind the course's student list.

        A course owned by someone else is reported as `COURSE_NOT_FOUND` (404) rather than 401, so this
        endpoint does not reveal whether another creator's course id exists.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Enrollments on the course. Empty when nobody has enrolled.",
        content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = Enrollment.class)))),
    @ApiResponse(responseCode = "400", description = "`courseId` was not a valid number (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "404", description = "No such course, or it belongs to another creator (`code: COURSE_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/{courseId}")
public ResponseEntity<List<Enrollment>> retrieveByCourse(
														 @Parameter(description = "Identifier of a course the caller owns.", required = true, example = "12")
														 @PathVariable("courseId") long courseId,
														 @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
	 String creatorId = jwt.getClaimAsString("sub");
  return ResponseEntity.status(HttpStatus.OK).body( this.enrollmentService.findByCourseId(courseId,creatorId));
}

}
