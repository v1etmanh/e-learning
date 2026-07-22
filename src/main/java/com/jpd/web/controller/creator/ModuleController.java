package com.jpd.web.controller.creator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.Module;
import com.jpd.web.service.ModuleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/api/creator/{courseId}/{chapterId}/module")
@Tag(name = "Course modules", description = "Modules within a chapter of a course the signed-in creator owns. The full course/chapter/module chain is validated on create and delete.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class ModuleController {
	@Autowired
	private ModuleService moduleService;

	@Operation(
	    summary = "Delete a module",
	    description = """
	        Removes a module and the content inside it. The whole chain is checked: the caller must own the
	        course, the chapter must belong to that course, and the module must belong to that chapter.
	        Irreversible.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "204", description = "Module deleted. Empty body.", content = @Content),
	    @ApiResponse(responseCode = "400", description = "A path variable was not positive, the chapter does not belong to the course, "
	            + "or the module does not belong to the chapter.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "401", description = "The course belongs to another creator, or the bearer token is missing or invalid.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "404", description = "No such course, chapter or module, or the caller has no creator profile.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@DeleteMapping("/{moduleId}")
	public ResponseEntity<?> deleteModule(
			@Positive(message = "Module ID must be positive")
			@Parameter(description = "Identifier of the module to delete. Must belong to the chapter.", required = true, example = "56")
			@PathVariable("moduleId") Long moduleId,
			@Positive(message = "Chapter ID must be positive")
			@Parameter(description = "Identifier of the chapter. Must belong to the course.", required = true, example = "34")
			@PathVariable("chapterId") Long chapterId,
			@Positive(message = "Course ID must be positive")
			@Parameter(description = "Identifier of a course the caller owns.", required = true, example = "12")
			@PathVariable("courseId") Long courseId,
			@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
			String creatorId=jwt.getClaimAsString("sub");
		moduleService.deleteModule(creatorId, courseId, chapterId, moduleId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content

	}

	@Operation(
	    summary = "Add a module to a chapter",
	    description = "Creates a module inside a chapter of a course the signed-in creator owns, and returns the saved entity.")
	@ApiResponses({
	    @ApiResponse(responseCode = "201", description = "Module created.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = Module.class))),
	    @ApiResponse(responseCode = "400", description = "A path variable was not positive, or the chapter does not belong to the course.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "401", description = "The course belongs to another creator, or the bearer token is missing or invalid.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "404", description = "No such course or chapter, or the caller has no creator profile.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "500", description = "`moduleName` was omitted, or another unexpected server error occurred.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping()
	public ResponseEntity<?> createModule(@Valid
			@Parameter(description = "Title of the new module.", required = true, example = "Bai 1: Bang chu cai Hiragana")
			@RequestParam("moduleName") String moduleName,
			@Positive(message = "Course ID must be positive")
			@Parameter(description = "Identifier of a course the caller owns.", required = true, example = "12")
			@PathVariable("courseId") Long courseId,
			@Positive(message = "Chapter ID must be positive")
			@Parameter(description = "Identifier of the chapter. Must belong to the course.", required = true, example = "34")
			@PathVariable("chapterId") Long chapterId,
			@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		// TODO: process POST request

		String creatorId=jwt.getClaimAsString("sub");

		// GlobalExceptionHandler sẽ xử lý exceptions
	Module module=	moduleService.createModule(moduleName, creatorId, courseId, chapterId);

		return ResponseEntity.status(HttpStatus.CREATED).body(module);

	}
	@Operation(
	    summary = "Rename a module",
	    description = """
	        Changes a module's title. Ownership is resolved from the module's own chapter and course, so
	        the `courseId` and `chapterId` in the path are not used — any values that parse will do.

	        **This endpoint does not currently work.** `ModuleService.updateModuleName` compares the
	        caller's id with the owning creator's id using reference inequality (`!=`) rather than
	        `equals`, and the two strings are never the same instance, so the ownership check fails even
	        for the rightful owner. Every call returns 401.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "204", description = "Module renamed. Empty body. Not currently reachable — see the description.", content = @Content),
	    @ApiResponse(responseCode = "400", description = "A path variable was not a valid number (`code: TYPE_MISMATCH`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "401", description = "Returned for every call because of the identity-comparison defect described above.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "404", description = "No module exists with this id (`code: MODULE_NOT_FOUND`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "500", description = "`name` was omitted, the caller has no creator profile, or another unexpected server error occurred.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PutMapping("/{moduleId}/update")
	public ResponseEntity<?> putMethodName(
										   @Parameter(description = "Identifier of the module to rename.", required = true, example = "56")
										   @PathVariable("moduleId") long moduleId,
										   @Parameter(description = "New module title.", required = true, example = "Bai 1: Hiragana co ban")
										   @RequestParam String name,
										   @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		//TODO: process PUT request
		String creatorId=jwt.getClaimAsString("sub");
		this.moduleService.updateModuleName(creatorId, moduleId, name);
		return ResponseEntity.noContent().build();
	}
}
