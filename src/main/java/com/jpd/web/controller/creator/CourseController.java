package com.jpd.web.controller.creator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;

import com.jpd.web.dto.PopularCourseDTO;

import com.jpd.web.model.Course;

import com.jpd.web.service.CourseService;

import com.jpd.web.transform.CourseTransForm;


import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/creator/course")
@Tag(name = "Creator courses", description = """
        Authoring and managing the signed-in creator's own courses.

        Ownership is checked per request against the token's `sub` claim: acting on a course you do not
        own is answered with 401, and having no creator profile at all gives 404.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class CourseController {
	@Autowired
	private CourseService courseService;

	@Operation(
	    summary = "Create a course",
	    description = """
	        Creates a course owned by the signed-in creator. The cover image, when supplied, is uploaded to
	        Firebase Storage.

	        The new course starts **unpublished** — publish it with `GET /api/creator/course/{id}/setCourseStatus`.
	        A join key is generated for `PRIVATE` courses and is returned on the response card.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "201", description = "Course created, unpublished.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseCardDto.class))),
	    @ApiResponse(responseCode = "400", description = "The cover image could not be uploaded (`code: FILE_UPLOAD_ERROR` or `API_ERROR`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    // Known gap: @Valid on @ModelAttribute raises BindException, which GlobalExceptionHandler does not
	    // map, so field-constraint violations land on the generic 500 handler instead of returning 400.
	    @ApiResponse(responseCode = "500", description = "Bean Validation failure on the form fields, or another unexpected server error.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CourseCardDto> createCourse(@Valid @ModelAttribute CourseFormDto entity,
													  @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		// TODO: process POST request
		Course c;

		String creatorId = jwt.getClaimAsString("sub");

			c = this.courseService.createCourse(entity, creatorId);
			return ResponseEntity.status(HttpStatus.CREATED).body(CourseTransForm.transformToCourseCardDto(c));
		

	}

	@Operation(
	    summary = "List the creator's own courses",
	    description = "Returns every course owned by the signed-in creator, published or not, each with its student and rating counts and its join key.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "The creator's courses. Empty when they have none.",
	        content = @Content(mediaType = "application/json",
	            array = @ArraySchema(schema = @Schema(implementation = CourseCardDto.class)))),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping()
	public ResponseEntity<List<CourseCardDto>> retrieveAll(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		
			String creatorId = jwt.getClaimAsString("sub");
			List<CourseCardDto> courses = this.courseService.retrieveCourseByemail(creatorId);
			return ResponseEntity.status(HttpStatus.OK).body(courses);
		
	}

	@Operation(
	    summary = "Get one of the creator's courses with its curriculum",
	    description = "Returns the chapter/module tree of a course the signed-in creator owns, with each module's content types resolved.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Course curriculum.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseContentDto.class))),
	    @ApiResponse(responseCode = "400", description = "`id` was not a positive number (`code: TYPE_MISMATCH` or a constraint violation).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "401", description = "The course belongs to another creator, or the bearer token is missing or invalid.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "404", description = "No such course, or the caller has no creator profile "
	            + "(`code: COURSE_NOT_FOUND` or `CREATOR_NOT_FOUND`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/{id}")
	public ResponseEntity<CourseContentDto> retrieveCourseById(
			 @Positive(message = "Course ID must be positive")
			@Parameter(description = "Identifier of a course the caller owns.", required = true, example = "12")
			@PathVariable("id") long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt ) {

		String creatorId = jwt.getClaimAsString("sub");
			return ResponseEntity.status(HttpStatus.OK).body(this.courseService.getCourseById(id, creatorId));
		

	}

	public CourseController() {
		// TODO Auto-generated constructor stub
	}

	
	@Operation(
	    summary = "Toggle a course between published and unpublished",
	    description = """
	        Flips the course's published flag: publishing an unpublished course, or withdrawing a published
	        one. There is no way to set a specific state — each call inverts the current one.

	        Despite being a state change this is mapped as `GET`, so it is neither safe nor idempotent
	        despite the verb. The response has no body.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Published state toggled. Empty body.", content = @Content),
	    @ApiResponse(responseCode = "400", description = "`id` was not a valid number (`code: TYPE_MISMATCH`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "401", description = "The course belongs to another creator, or the bearer token is missing or invalid.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "404", description = "No such course, or the caller has no creator profile "
	            + "(`code: COURSE_NOT_FOUND` or `CREATOR_NOT_FOUND`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/{id}/setCourseStatus")
	public ResponseEntity<?> putMethodName(
			@Parameter(description = "Identifier of a course the caller owns.", required = true, example = "12")
			@PathVariable long id,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		//TODO: process PUT request
		String creatorId = jwt.getClaimAsString("sub");
		
		this.courseService.changeCourseSatus(id, creatorId);
		return ResponseEntity.status(HttpStatus.OK).build();
	}
	@Operation(
	    summary = "List the creator's paid courses",
	    description = "Returns the signed-in creator's commercial (non-free) courses with their student counts and ratings — the data behind the earnings dashboard.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "The creator's paid courses. Empty when they have none.",
	        content = @Content(mediaType = "application/json",
	            array = @ArraySchema(schema = @Schema(implementation = PopularCourseDTO.class)))),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/retrieve_CommercialCourese")
	public ResponseEntity<List<PopularCourseDTO>>retrieveCCourse(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		String creatorId = jwt.getClaimAsString("sub");
		List<PopularCourseDTO>cpp=this.courseService.retrieveCCourse(creatorId);
		return ResponseEntity.ok(cpp);
	}


}
