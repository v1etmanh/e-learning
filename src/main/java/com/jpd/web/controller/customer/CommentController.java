package com.jpd.web.controller.customer;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.CommentDto;
import com.jpd.web.dto.CommentRequestDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/courses/{courseId}/comments")
@Slf4j
@Tag(name = "Course comments", description = """
        Discussion under a course. Reading and posting require the caller to be enrolled in the course or
        be its creator.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * Get all comments of a course
     * GET /api/courses/{courseId}/comments
     */
    @Operation(
        summary = "List a course's comments",
        description = """
            Returns every comment on the course, oldest first.

            The caller must be enrolled in the course or be its creator. Because the `Comment` entity does
            not store an author reference, `createBy` is the literal string `Anonymous` on every item —
            this endpoint cannot tell you who wrote what.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All comments on the course. Empty when there are none.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = CommentDto.class)),
                examples = @ExampleObject(name = "comments", value = """
                    [
                      { "comment": "Bài 12 giải thích rất rõ, cảm ơn thầy!", "createBy": "Anonymous" },
                      { "comment": "Phần ngữ pháp て form mình vẫn chưa hiểu lắm.", "createBy": "Anonymous" }
                    ]"""))),
        @ApiResponse(responseCode = "400", description = "`courseId` was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The caller is neither enrolled nor the creator, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<CommentDto>> getAllComments(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identifier of the course.", required = true, example = "12")
            @PathVariable("courseId") long courseId) {
        
        log.debug("Getting all comments for course {}", courseId);
        String customerId = jwt.getClaimAsString("sub");
        
        List<CommentDto> comments = commentService.retrieveAllCommentOfCourse(courseId, customerId);
        return ResponseEntity.ok(comments);
    }
    
    /**
     * Create a new comment
     * POST /api/courses/{courseId}/comments
     */
    @Operation(
        summary = "Post a comment on a course",
        description = """
            Adds a comment to the course. The author is taken from the `sub` claim of the bearer token.

            The caller must be enrolled in the course or be its creator. The text is screened by the
            toxicity filter before it is saved.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comment created. `createBy` is the caller's Keycloak user id.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CommentDto.class),
                examples = @ExampleObject(name = "created", value = """
                    {
                      "comment": "Bài 12 giải thích rất rõ, cảm ơn thầy!",
                      "createBy": "9c2e5f77-11ab-42d0-8b3e-6ce0d4a9e777"
                    }"""))),
        @ApiResponse(responseCode = "400", description = "`content` was blank or longer than 1000 characters (`code: VALIDATION_ERROR`), "
                + "or the text was flagged as toxic (`code: FEEDBACK_ILLEGAL`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The caller is neither enrolled nor the creator, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identifier of the course to comment on.", required = true, example = "12")
            @PathVariable("courseId") long courseId,
            @Valid @RequestBody CommentRequestDto request) {
        
        log.debug("Creating comment for course {}", courseId);
        String customerId = jwt.getClaimAsString("sub");
        
        CommentDto comment = commentService.createComment(customerId, request.getContent(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }
    
    /**
     * Update a comment
     * PUT /api/courses/{courseId}/comments/{commentId}
     */
    @Operation(
        summary = "Edit a comment",
        description = """
            Replaces the text of an existing comment.

            **No authorization is performed.** Any authenticated caller can edit any comment by id — the
            service looks the comment up and saves the new content without checking ownership or
            enrollment. The `courseId` in the path is not checked against the comment either, so a
            mismatched course id still succeeds. The text is not screened by the toxicity filter on update.

            The returned `createBy` is the caller's id, not the original author's.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comment updated.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentDto.class))),
        @ApiResponse(responseCode = "400", description = "`content` was blank or longer than 1000 characters (`code: VALIDATION_ERROR`), "
                + "or a path variable was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No comment exists with this id, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identifier of the course. Not validated against the comment.", required = true, example = "12")
            @PathVariable("courseId") long courseId,
            @Parameter(description = "Identifier of the comment to edit.", required = true, example = "301")
            @PathVariable("commentId") long commentId,
            @Valid @RequestBody CommentRequestDto request) {
        
        log.debug("Updating comment {} for course {}", commentId, courseId);
        String customerId = jwt.getClaimAsString("sub");
        
        CommentDto comment = commentService.updateComment(commentId, customerId, request.getContent());
        return ResponseEntity.ok(comment);
    }
    
    /**
     * Delete a comment
     * DELETE /api/courses/{courseId}/comments/{commentId}
     */
    @Operation(
        summary = "Delete a comment",
        description = """
            Removes a comment. Only the creator of the course the comment belongs to may delete it —
            learners cannot delete their own comments, because the `Comment` entity has no author
            reference to check against.

            The `courseId` in the path is not validated against the comment; ownership is resolved from
            the comment's own course.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment deleted. Empty body.", content = @Content),
        @ApiResponse(responseCode = "400", description = "A path variable was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The caller is not the creator of the comment's course, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "No comment exists with this id, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identifier of the course. Not validated against the comment.", required = true, example = "12")
            @PathVariable("courseId") long courseId,
            @Parameter(description = "Identifier of the comment to delete.", required = true, example = "301")
            @PathVariable("commentId") long commentId) {
        
        log.debug("Deleting comment {} from course {}", commentId, courseId);
        String customerId = jwt.getClaimAsString("sub");
        
        commentService.deleteCommentById(commentId, customerId);
        return ResponseEntity.noContent().build();
    }
}