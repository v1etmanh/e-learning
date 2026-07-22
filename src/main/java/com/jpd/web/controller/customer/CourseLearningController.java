package com.jpd.web.controller.customer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.CourseLearningService;
import com.jpd.web.service.CourseService;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/customer/learning/{courseId}")
@Tag(name = "Course learning", description = """
        Studying an enrolled course: browsing its curriculum, loading module content, and marking content finished.

        Access is granted to the course creator or to an enrolled customer. Everyone else gets 401.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class CourseLearningController {


@Autowired
private CourseLearningService courseLearningService;



@Operation(
    summary = "Get the course curriculum",
    description = """
        Returns the chapter/module tree of a course, with the set of content types resolved for each
        module, so the client can render the course outline.

        The caller is taken from the `sub` claim of the bearer token. The course creator always has
        access to their own course, published or not. Anyone else must be enrolled, and additionally
        the course must be published — an enrolled customer on an unpublished course gets 401.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Course curriculum.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseContentDto.class))),
    @ApiResponse(responseCode = "400", description = "`courseId` was not a valid number (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The caller is neither the creator nor enrolled, the course is not published, "
            + "or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/courseOverview")
public ResponseEntity<CourseContentDto> getMethodName(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
		@Parameter(description = "Identifier of the course being studied.", required = true, example = "12")
		@PathVariable("courseId")long courseId) {
    CourseContentDto contentDto=this.courseLearningService.getCourseById(courseId, jwt.getClaimAsString("sub"));
    return ResponseEntity.ok(contentDto);
}
@Operation(
    summary = "Load a module's content of one type",
    description = """
        Returns the learning content items of a single type inside a module — the flashcards, the
        multiple-choice questions, the video, and so on.

        The full ownership chain is validated: the course must exist, the caller must be its creator or
        be enrolled, the chapter must belong to the course, and the module must belong to the chapter.

        An empty array means the module simply holds no content of the requested type.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Content items of the requested type. Empty when the module has none of that type.",
        content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = ModuleContent.class)))),
    @ApiResponse(responseCode = "400", description = "A path variable was not a valid number, or `typeOfContent` is not one of the allowed values (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The caller is neither the creator nor enrolled, or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "The course, chapter or module does not exist "
            + "(`code: COURSE_NOT_FOUND`, `CHAPTER_NOT_FOUND` or `MODULE_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "`typeOfContent` was omitted, the chapter does not belong to the course, "
            + "the module does not belong to the chapter, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/{chapterId}/{moduleId}/moduleContent")
public ResponseEntity<?> retrieveModuleContent(
		@Parameter(description = "Which kind of content to load from the module.", required = true, example = "FLASHCARD")
		@RequestParam("typeOfContent")TypeOfContent typeOfContent,
		@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
		@Parameter(description = "Identifier of the course.", required = true, example = "12")
		@PathVariable("courseId")long courseId ,
		@Parameter(description = "Identifier of the chapter. Must belong to the course.", required = true, example = "34")
		@PathVariable("chapterId")long chapterId,
		@Parameter(description = "Identifier of the module. Must belong to the chapter.", required = true, example = "56")
		@PathVariable("moduleId")long moduleId){
	String customerId=jwt.getClaimAsString("sub");
	List<ModuleContent>mds=this.courseLearningService.getModuleContentsByTypeAndModuleId(typeOfContent, moduleId, chapterId, courseId, customerId);
	return ResponseEntity.ok(mds);
	
}
@Operation(
    summary = "Mark a module's content type as finished",
    description = """
        Records that the signed-in customer has completed one content type within a module. This is what
        drives the `progress` percentage on `GET /api/customer/learning_course_list`.

        Marking the same content type twice is harmless — completions are held in a set, so a repeat
        call is a no-op and still returns 204.

        The customer must be enrolled in the course, the module must belong to that course, and the
        module must actually contain the given content type.

        Note: this endpoint is not usable by the course creator. `validateCustomerWithCourseGetE`
        returns `null` for a creator, which the service then dereferences, so a creator calling it on
        their own course gets 500.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "Completion recorded. Empty body.", content = @Content),
    @ApiResponse(responseCode = "400", description = "A path variable was not a valid number, or `type` is not one of the allowed values (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The customer is not enrolled in the course, the module belongs to a different course, "
            + "or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "The course or module does not exist (`code: COURSE_NOT_FOUND` or `MODULE_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "The module does not contain the requested content type, the caller is the course creator, "
            + "`type` was omitted, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/{moduleId}/finish_content")
public ResponseEntity<?> updateCustomerWithModuleContent(
		@Parameter(description = "Identifier of the enrolled course.", required = true, example = "12")
		@PathVariable("courseId")long courseId,
		@Parameter(description = "Identifier of the module. Must belong to the course.", required = true, example = "56")
		@PathVariable("moduleId")long moduleId,
		@Parameter(description = "Which content type of the module was completed. Must be a type the module actually holds.",
				required = true, example = "FLASHCARD")
		@RequestParam("type") TypeOfContent typeOfContent,
		@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)
{ String customerId=jwt.getClaimAsString("sub");
		this.courseLearningService.updateCustomerFinishModule(courseId, customerId, moduleId, typeOfContent);
		return ResponseEntity.noContent().build();
	}
}
