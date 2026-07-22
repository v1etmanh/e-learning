package com.jpd.web.controller.creator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.service.ModuleContentService;

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
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/api/creator/{courseId}/{chapterId}/{moduleId}")
@Slf4j
@Tag(name = "Module content", description = """
        The learning material inside a module — flashcards, questions, videos and so on — authored by the
        course's creator.

        Ownership is resolved from the module alone; the `courseId` and `chapterId` path variables are
        required by the route but are not checked against the module.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class ModuleContentController {
    @Autowired
    private ModuleContentService moduleContentService;

    @Operation(
        summary = "List a module's content of one type",
        description = "Returns the content items of a single type inside a module the signed-in creator owns.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Content items of the requested type. Empty when the module holds none of that type.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ModuleContent.class)))),
        @ApiResponse(responseCode = "400", description = "A path variable was not positive, or `type` is not one of the allowed values (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The module belongs to another creator's course, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No such module, or the caller has no creator profile.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "`type` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping()
    public ResponseEntity<List<ModuleContent>> getModuleContentByTypeAndModuleId(
                                                                                 @Parameter(description = "Which kind of content to list.", required = true, example = "FLASHCARD")
                                                                                 @RequestParam("type") TypeOfContent typeOfContent,
                                                                                 @Positive
                                                                                 @Parameter(description = "Identifier of the module.", required = true, example = "56")
                                                                                 @PathVariable("moduleId") Long moduleId,
                                                                                 @Positive
                                                                                 @Parameter(description = "Identifier of the chapter. Required by the route but not validated against the module.", required = true, example = "34")
                                                                                 @PathVariable("chapterId") Long chapterId,
                                                                                 @Positive
                                                                                 @Parameter(description = "Identifier of the course. Required by the route but not validated against the module.", required = true, example = "12")
                                                                                 @PathVariable("courseId") Long courseId,
                                                                                 @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        //return
        String creatorId=jwt.getClaimAsString("sub");
        List<ModuleContent>mds=moduleContentService.getModuleContentsByType(typeOfContent,moduleId,creatorId);

        return ResponseEntity.ok().body(mds);
    }
    @Operation(
        summary = "Delete one content item",
        description = """
            Removes a single content item from a module the signed-in creator owns. The item must belong
            to the module named in the path. The response has no body.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Content item deleted. Empty body.", content = @Content),
        @ApiResponse(responseCode = "400", description = "A path variable was not positive (`code: TYPE_MISMATCH` or a constraint violation).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The module belongs to another creator's course, the item does not belong to the module, "
                + "or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No such module or content item, or the caller has no creator profile.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{moduleContentId}")
    public ResponseEntity<?> deleteModuleContent(    @Positive
                                                     @Parameter(description = "Identifier of the content item to delete. Must belong to the module.", required = true, example = "9042")
                                                     @PathVariable("moduleContentId") long moduleContentId,
                                                     @Positive
                                                     @Parameter(description = "Identifier of the module.", required = true, example = "56")
                                                     @PathVariable("moduleId") Long moduleId,
                                                     @Positive
                                                     @Parameter(description = "Identifier of the chapter. Not validated against the module.", required = true, example = "34")
                                                     @PathVariable("chapterId") Long chapterId,
                                                     @Positive
                                                     @Parameter(description = "Identifier of the course. Not validated against the module.", required = true, example = "12")
                                                     @PathVariable("courseId") Long courseId,
                                                     @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt               ) {

        String creatorId=jwt.getClaimAsString("sub");
        moduleContentService.deleteModuleContent(
                moduleContentId, moduleId, creatorId
        );

        return ResponseEntity.ok().build();

    }
    @Operation(
        summary = "Delete all of a module's content of one type",
        description = """
            Removes every content item of the given type from a module the signed-in creator owns — for
            example, clearing all flashcards while leaving the video in place. Irreversible.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "All matching content deleted. Empty body. Also returned when the module held none of that type.",
            content = @Content),
        @ApiResponse(responseCode = "400", description = "A path variable was not positive, or `type` is not one of the allowed values (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The module belongs to another creator's course, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No such module, or the caller has no creator profile.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "`type` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/deleteModuleContentByType")
    public ResponseEntity<?> deleteModuleContentByType(
                                                       @Parameter(description = "Which kind of content to clear from the module.", required = true, example = "FLASHCARD")
                                                       @RequestParam("type") TypeOfContent type,
                                                       @Positive
                                                       @Parameter(description = "Identifier of the module.", required = true, example = "56")
                                                       @PathVariable ("moduleId")long moduleId,
                                                       @Positive
                                                       @Parameter(description = "Identifier of the chapter. Not validated against the module.", required = true, example = "34")
                                                       @PathVariable ("chapterId")long chapterId,
                                                       @Positive
                                                       @Parameter(description = "Identifier of the course. Not validated against the module.", required = true, example = "12")
                                                       @PathVariable ("courseId")long courseId,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        String creatorId=jwt.getClaimAsString("sub");
        moduleContentService.deleteModuleContentsByType( type, moduleId, creatorId);
        return ResponseEntity.noContent().build();

    }
    @Operation(
        summary = "Replace a module's content",
        description = """
            Saves the supplied content items into the module named in the request body.

            This is a **replace, not a merge**: every item you send is inserted fresh, and any item you
            send that already had an `mcId` is deleted and re-inserted, so it comes back with a *new*
            identifier. Items already in the module that you do not send are left untouched. Sending an
            empty or absent `moduleContent` list is a no-op and returns an empty array.

            The `courseId`, `chapterId` and `moduleId` in the path are ignored — the target module is the
            `moduleId` inside the body.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "The target module and the content items to store in it.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ModuleContentDto.class)))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The saved content items, with their newly assigned identifiers.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ModuleContent.class)))),
        @ApiResponse(responseCode = "401", description = "The module belongs to another creator's course, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No module exists with the body's `moduleId`, or the caller has no creator profile.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Malformed or missing request body, or another unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<?> updateModuleContents(
            @Valid @RequestBody ModuleContentDto moduleContentDto,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        String creatorId=jwt.getClaimAsString("sub");
         var result = moduleContentService.updateCourseMaterial(moduleContentDto, creatorId);
        return ResponseEntity.ok(result);
    }
}