package com.jpd.web.controller.creator;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.KahootDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.service.KahootService;
import com.jpd.web.transform.KahootTransform;

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

import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/api/creator/kahoot")
@Tag(name = "Kahoot quizzes", description = """
        Quiz sets owned by the signed-in creator. A Kahoot is the template; running one live is done
        through the Live quiz endpoints.

        Every call validates that the Kahoot belongs to the token's `sub` claim.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class KahootController {
	@Autowired
	private KahootService kahootService;

@Operation(
    summary = "List the creator's Kahoots",
    description = "Returns every quiz set owned by the signed-in creator, each with its question count.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The creator's Kahoots. Empty when they have none.",
        content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = KahootDto.class)),
            examples = @ExampleObject(name = "kahoots", value = """
                [
                  {
                    "title": "On tap Kanji N5",
                    "createDate": "2026-07-01T14:22:10.000",
                    "numberQuestion": 15,
                    "id": 77
                  }
                ]"""))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/retrieveAll")
public ResponseEntity<List<KahootDto>> retrieveAllByCreatorId(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
	String creatorId=jwt.getClaimAsString("sub");
	List<KahootDto> khs=this.kahootService.retrieveAll(creatorId);
	return ResponseEntity.ok(khs);
}
@Operation(
    summary = "List a Kahoot's questions",
    description = "Returns the question content items that make up a Kahoot the signed-in creator owns.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The Kahoot's questions. Empty when it has none.",
        content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = ModuleContent.class)))),
    @ApiResponse(responseCode = "400", description = "`kahootId` was not a valid number (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The Kahoot belongs to another creator, or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "No Kahoot exists with this id, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/{kahootId}/moduleContents")
public ResponseEntity<List<ModuleContent>> getDataOfKahoot(
		@Parameter(description = "Identifier of a Kahoot the caller owns.", required = true, example = "77")
		@PathVariable("kahootId")long kahootId,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)
{   String creatorId=jwt.getClaimAsString("sub");
List<ModuleContent> khs=this.kahootService.retrieveData(creatorId,kahootId);
return ResponseEntity.ok(khs);
	}
@Operation(
    summary = "Create a Kahoot",
    description = "Creates an empty quiz set owned by the signed-in creator. Add questions afterwards through the Kahoot module-content endpoints.")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Kahoot created, with no questions yet.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = KahootDto.class),
            examples = @ExampleObject(name = "created", value = """
                {
                  "title": "On tap Kanji N5",
                  "createDate": "2026-07-22T09:15:30.412",
                  "numberQuestion": 0,
                  "id": 77
                }"""))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "`title` was omitted, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/create")
public ResponseEntity<?>createKahoot(
		@Parameter(description = "Title of the new quiz set.", required = true, example = "On tap Kanji N5")
		@RequestParam("title")String name,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)
{  String creatorId=jwt.getClaimAsString("sub");
	KahootDto k=KahootTransform.transformToKahootDto(this.kahootService.createKahoot(name, creatorId));
	return ResponseEntity.status(HttpStatus.CREATED).body(k);
	}
@Operation(
    summary = "Delete a Kahoot",
    description = "Removes a quiz set owned by the signed-in creator, along with its questions. Irreversible, and does not affect sessions already running from it.")
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "Kahoot deleted. Empty body.", content = @Content),
    @ApiResponse(responseCode = "400", description = "`kahootId` was not a valid number (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The Kahoot belongs to another creator, or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "No Kahoot exists with this id, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@DeleteMapping("/{kahootId}")
public ResponseEntity<?>deleteKahoot(
		@Parameter(description = "Identifier of a Kahoot the caller owns.", required = true, example = "77")
		@PathVariable("kahootId")long kahootId,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)
{  String creatorId=jwt.getClaimAsString("sub");
   this.kahootService.deleteKahoot(creatorId, kahootId);
   return ResponseEntity.noContent().build();
	}
@Operation(
    summary = "Rename a Kahoot",
    description = "Changes the title of a quiz set owned by the signed-in creator. The response has no body.")
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "Kahoot renamed. Empty body.", content = @Content),
    @ApiResponse(responseCode = "400", description = "`kahootId` was not a valid number (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The Kahoot belongs to another creator, or the bearer token is missing or invalid.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "No Kahoot exists with this id, `newTitle` was omitted, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PutMapping("/{kahootId}")
public ResponseEntity<?> putMethodName(
		@Parameter(description = "Identifier of a Kahoot the caller owns.", required = true, example = "77")
		@PathVariable long kahootId,
		@Parameter(description = "New quiz title.", required = true, example = "On tap Kanji N5 - ban cap nhat")
		@RequestParam("newTitle")String t,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    //TODO: process PUT request
	String creatorId=jwt.getClaimAsString("sub");
	   
    this.kahootService.updateKahootTitle(creatorId, kahootId, t);
    return ResponseEntity.noContent().build();
}
}
