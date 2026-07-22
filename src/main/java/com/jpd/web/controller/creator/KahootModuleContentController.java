package com.jpd.web.controller.creator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.service.KahootModuleContentService;

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

@RestController
@RequestMapping("/api/creator/kahootModuleContent/{kahootId}")
@Tag(name = "Kahoot questions", description = "The questions inside a Kahoot owned by the signed-in creator. Ownership of the Kahoot is validated on every call.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class KahootModuleContentController {
@Autowired
private KahootModuleContentService kahootModuleContentService;

@Operation(
    summary = "Delete one question from a Kahoot",
    description = "Removes a single question from a Kahoot the signed-in creator owns. The question must belong to that Kahoot. The response has no body.")
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "Question deleted. Empty body.", content = @Content),
    @ApiResponse(responseCode = "400", description = "A path variable was not positive (`code: TYPE_MISMATCH` or a constraint violation).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The Kahoot belongs to another creator, the question does not belong to it, "
            + "or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "No such question, or the caller has no creator profile "
            + "(`code: MODULE_CONTENT_NOT_FOUND` or `CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "No Kahoot exists with this id, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@DeleteMapping("/{moduleContentId}")
public ResponseEntity<?> deleteModuleContent(@Positive
                                             @Parameter(description = "Identifier of the question to delete. Must belong to the Kahoot.", required = true, example = "9042")
                                             @PathVariable("moduleContentId") long moduleContentId,
                                             @Positive
                                             @Parameter(description = "Identifier of a Kahoot the caller owns.", required = true, example = "77")
                                             @PathVariable("kahootId") Long kahootId,

                                             @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

    String creatorId = jwt.getClaimAsString("sub");
    kahootModuleContentService.deleteModuleContent(
    		moduleContentId, kahootId, creatorId
    );
    
    return ResponseEntity.noContent().build();
  
}

@Operation(
    summary = "Save questions into a Kahoot",
    description = """
        Stores the supplied questions in a Kahoot the signed-in creator owns.

        Every item you send is inserted fresh. An item carrying an existing `mcId` is deleted and
        re-inserted, so it comes back with a **new** identifier — update your local copies from the
        response. Questions already in the Kahoot that you do not send are left untouched. Send `mcId`
        as `null` or a negative number for a brand-new question.
        """)
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    required = true,
    description = "The questions to store. Only `MULTIPLE_CHOICE` and `GAPFILL` items are playable in a live session.",
    content = @Content(mediaType = "application/json",
        array = @ArraySchema(schema = @Schema(implementation = ModuleContent.class))))
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The saved questions, with their newly assigned identifiers.",
        content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = ModuleContent.class)))),
    @ApiResponse(responseCode = "400", description = "`kahootId` was not positive, or a question failed Bean Validation (`code: VALIDATION_ERROR`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The Kahoot belongs to another creator, or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "No Kahoot exists with this id, the body was malformed, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping
public ResponseEntity<?> updateModuleContents(
        @Valid @RequestBody List<ModuleContent>mds,
        @Positive
        @Parameter(description = "Identifier of a Kahoot the caller owns.", required = true, example = "77")
        @PathVariable("kahootId") Long kahootId,
        @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

    String creatorId = jwt.getClaimAsString("sub");
    List<ModuleContent> contents = this.kahootModuleContentService.updateCourseMaterial(mds,kahootId, creatorId);
    return ResponseEntity.ok(contents);
}
}
