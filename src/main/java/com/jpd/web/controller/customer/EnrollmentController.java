package com.jpd.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.EnrollmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/enroll")
@Tag(name = "Enrollment", description = "Joining a course as a learner.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class EnrollmentController {
@Autowired
private EnrollmentService enrollmentService;

	@Operation(
	    summary = "Enroll in a course",
	    description = """
	        Enrolls the signed-in customer in a course and increments the course's enrollment count.

	        The customer is taken from the `sub` claim of the bearer token. For a course whose access mode
	        is `PRIVATE`, `joinKey` must match the course's join key; a wrong key yields 400 with an empty
	        body. For a public course the `joinKey` value is ignored — but the query parameter is still
	        mandatory and the request fails without it.

	        Both success and the wrong-key case return an empty body.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Enrolled. Empty body.", content = @Content),
	    @ApiResponse(responseCode = "400", description = "The course is `PRIVATE` and `joinKey` did not match. Empty body.",
	        content = @Content),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "404", description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "409", description = "The customer is already enrolled in this course.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "500", description = "`joinKey` was omitted, or another unexpected server error occurred.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/{id}")
	public ResponseEntity<?> enroll(
			@Parameter(description = "Join key for a `PRIVATE` course. Required on every request, but ignored for public courses — pass an empty string there.",
					required = true, example = "N5-2026-SPRING")
			@RequestParam("joinKey") String key,
			@Parameter(description = "Identifier of the course to enroll in.", required = true, example = "12")
			@PathVariable("id")long courseId,
			@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
	    //TODO: process POST request
		String customerId = jwt.getClaimAsString("sub");
	    boolean a=this.enrollmentService.handleEnrollCouse(courseId, key, customerId);
	    if(a) return ResponseEntity.status(HttpStatus.OK).build();
	    else return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
	
}
