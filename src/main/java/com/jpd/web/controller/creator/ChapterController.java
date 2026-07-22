package com.jpd.web.controller.creator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.Chapter;
import com.jpd.web.service.ChapterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/creator/{courseId}/chapter")
@RequiredArgsConstructor
@Tag(name = "Course chapters", description = "Chapters within a course the signed-in creator owns. Ownership is checked against the token's `sub` claim on every call.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class ChapterController {

    private final ChapterService chapterService;

    @Operation(
        summary = "Add a chapter to a course",
        description = "Creates a chapter in a course the signed-in creator owns and returns the saved entity.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chapter created.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Chapter.class))),
        @ApiResponse(responseCode = "400", description = "`chapterName` was blank or `courseId` was not positive.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The course belongs to another creator, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No such course, or the caller has no creator profile "
                + "(`code: COURSE_NOT_FOUND` or `CREATOR_NOT_FOUND`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "`chapterName` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<?> createChapter(
            @NotBlank(message = "Chapter name is required")
            @Parameter(description = "Title of the new chapter.", required = true, example = "Chuong 1: Hiragana")
            @RequestParam("chapterName") String name,
            @Positive(message = "Course ID must be positive")
            @Parameter(description = "Identifier of a course the caller owns.", required = true, example = "12")
            @PathVariable("courseId") Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Creating chapter '{}' for course {}", name, courseId);
        
        String creatorId = jwt.getClaimAsString("sub");
        
        // ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
        Chapter chapter = chapterService.createChapter(name, courseId, creatorId);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(chapter);
    }
    @Operation(
        summary = "Delete a chapter",
        description = """
            Removes a chapter from a course the signed-in creator owns. The chapter must belong to the
            course named in the path.

            Deleting a chapter removes its modules and their content with it. Irreversible.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Chapter deleted. Empty body.", content = @Content),
        @ApiResponse(responseCode = "400", description = "A path variable was not positive, or the chapter does not belong to the course "
                + "(`code: CHAPTER_NOT_BELONGS_TO_COURSE`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The course belongs to another creator, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No such course or chapter, or the caller has no creator profile.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @Positive(message = "Chapter ID must be positive")
            @Parameter(description = "Identifier of the chapter to delete. Must belong to the course.", required = true, example = "34")
            @PathVariable("chapterId") Long chapterId,
            @Positive(message = "Course ID must be positive")
            @Parameter(description = "Identifier of a course the caller owns.", required = true, example = "12")
            @PathVariable("courseId") Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Deleting chapter {} from course {}", chapterId, courseId);
        
        String creatorId= jwt.getClaimAsString("sub");
        
        // ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
        chapterService.deleteChapter(chapterId, courseId, creatorId);
        
        return ResponseEntity.noContent().build();
    }
    @Operation(
        summary = "Rename a chapter",
        description = """
            Changes a chapter's title. Ownership is resolved from the chapter's own course, so the
            `courseId` in the path is not used at all — any value that parses will do.

            A missing chapter is reported as `MODULE_NOT_FOUND`, not `CHAPTER_NOT_FOUND`. The response
            has no body.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Chapter renamed. Empty body.", content = @Content),
        @ApiResponse(responseCode = "400", description = "A path variable was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The chapter's course belongs to another creator, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No chapter exists with this id — reported as `code: MODULE_NOT_FOUND`.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "`name` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{chapterID}/update")
    public ResponseEntity<?>updateChapter(
    		@Parameter(description = "New chapter title.", required = true, example = "Chuong 1: Hiragana va Katakana")
    		@RequestParam String name ,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
    		@Parameter(description = "Identifier of the chapter to rename.", required = true, example = "34")
    		@PathVariable("chapterID")long chapterId){
  
    	String creatorId=jwt.getClaimAsString("sub");
    	this.chapterService.updateChapter(creatorId, name, chapterId);
    	return ResponseEntity.noContent().build();
    }


   
   
}