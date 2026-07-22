package com.jpd.web.voice;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/voice")
@Tag(name = "Voice practice", description = """
        Spoken-conversation practice with AI characters, powered by Gemini Live, plus tracking of the
        learner's daily speaking goal.

        The audio conversation itself does **not** go through this API: the client asks for a short-lived
        token here and then connects to Gemini Live directly.

        Known gap: this controller signals errors with `ResponseStatusException`, but
        `GlobalExceptionHandler` has a catch-all `@ExceptionHandler(Exception.class)` which runs before
        Spring's own status resolver. The intended 401 and 404 responses are therefore converted into
        **500** with `code: INTERNAL_ERROR`; the intended status survives only inside the message text.
        The 401 and 404 codes below are documented as intent — read them as 500 until the handler is fixed.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class VoiceLiveController {
    private final VoiceCharacterCatalog catalog;
    private final GeminiLiveTokenService tokenService;
    private final VoiceConversationProgressService progressService;

    public VoiceLiveController(VoiceCharacterCatalog catalog, GeminiLiveTokenService tokenService,
            VoiceConversationProgressService progressService) {
        this.catalog = catalog;
        this.tokenService = tokenService;
        this.progressService = progressService;
    }

    @Operation(
        summary = "List available conversation characters",
        description = "Returns every character the learner can practise speaking with. The catalogue is static and identical for all callers.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All available characters.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = VoiceCharacterSummary.class)),
                examples = @ExampleObject(name = "characters", value = """
                    [
                      {
                        "id": "tanaka-sensei",
                        "displayName": "Tanaka Sensei",
                        "description": "Giao vien tieng Nhat kien nhan, noi cham va sua loi phat am.",
                        "category": "teacher",
                        "liveVoice": "Kore"
                      }
                    ]"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content)
    })
    @GetMapping("/characters")
    public ResponseEntity<List<VoiceCharacterSummary>> characters() {
        return ResponseEntity.ok(catalog.all().stream().map(VoiceCharacterSummary::from).toList());
    }

    @Operation(
        summary = "Get one conversation character",
        description = "Returns a single character from the catalogue by its identifier.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The character.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoiceCharacterSummary.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Intended response when no character has this id. Currently delivered as 500 - see the tag description.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "What an unknown `characterId` actually returns today, and any other unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/characters/{characterId}")
    public ResponseEntity<VoiceCharacterSummary> character(
            @Parameter(description = "Identifier of the character, as returned by `GET /api/voice/characters`.", required = true, example = "tanaka-sensei")
            @PathVariable String characterId) {
        VoiceCharacterProfile profile = catalog.get(characterId);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voice character not found");
        }
        return ResponseEntity.ok(VoiceCharacterSummary.from(profile));
    }

    @Operation(
        summary = "Get a token for a live voice session",
        description = """
            Issues short-lived Gemini Live credentials scoped to one character. The client then opens the
            audio session against Gemini directly — no audio passes through this API.

            The token expires; request a new one rather than caching it. Treat it as a secret.

            This endpoint only mints a token. Tracking the conversation towards the daily goal is separate
            — see `POST /api/voice/progress/sessions`.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = VoiceLiveStartRequest.class),
            examples = @ExampleObject(name = "token", value = """
                { "characterId": "tanaka-sensei" }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Live credentials for the requested character.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VoiceLiveTokenResponse.class),
                examples = @ExampleObject(name = "issued", value = """
                    {
                      "accessToken": "auth_tokens/abc123def456",
                      "model": "gemini-2.0-flash-live-001",
                      "characterId": "tanaka-sensei",
                      "expiresAt": "2026-07-22T09:45:30Z"
                    }"""))),
        @ApiResponse(responseCode = "400", description = "`characterId` was blank (`code: VALIDATION_ERROR`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Intended response when no character has this id. Currently delivered as 500 - see the tag description.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "What an unknown `characterId` actually returns today, or the token could not be minted.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/live/token")
    public ResponseEntity<VoiceLiveTokenResponse> createLiveToken(
            @Valid @RequestBody VoiceLiveStartRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        VoiceCharacterProfile profile = catalog.get(request.characterId());
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voice character not found");
        }
        return ResponseEntity.ok(tokenService.createToken(profile));
    }

    @Operation(
        summary = "Start tracking a practice conversation",
        description = """
            Opens a tracking record for a conversation the learner is about to have, so it can count
            towards their daily speaking goal. Call it alongside opening the Gemini Live session, then
            close it with the complete endpoint.

            The returned `id` is what you pass to `POST /api/voice/progress/sessions/{sessionId}/complete`.
            A session left uncompleted never counts towards the daily goal.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = VoiceConversationStartRequest.class),
            examples = @ExampleObject(name = "start", value = """
                { "characterId": "tanaka-sensei" }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tracking started.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VoiceConversationSessionResponse.class),
                examples = @ExampleObject(name = "started", value = """
                    {
                      "id": 418,
                      "characterId": "tanaka-sensei",
                      "startedAt": "2026-07-22T09:15:30.412",
                      "endedAt": null,
                      "durationSeconds": 0,
                      "completed": false
                    }"""))),
        @ApiResponse(responseCode = "400", description = "`characterId` was blank (`code: VALIDATION_ERROR`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Intended response when no character has this id. Currently delivered as 500 - see the tag description.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "What an unknown `characterId` actually returns today, and any other unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/progress/sessions")
    public ResponseEntity<VoiceConversationSessionResponse> startConversationTracking(
            @Valid @RequestBody VoiceConversationStartRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String userId = requireUserId(jwt);
        if (catalog.get(request.characterId()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voice character not found");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VoiceConversationSessionResponse.from(progressService.start(userId, request.characterId())));
    }

    @Operation(
        summary = "Finish tracking a practice conversation",
        description = """
            Closes a tracking record, stamps its end time and computes its duration. Only once completed
            does the conversation count towards the daily speaking goal.

            The session must belong to the signed-in learner; someone else's session is treated as not
            found rather than forbidden.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tracking completed, with the measured duration.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VoiceConversationSessionResponse.class),
                examples = @ExampleObject(name = "completed", value = """
                    {
                      "id": 418,
                      "characterId": "tanaka-sensei",
                      "startedAt": "2026-07-22T09:15:30.412",
                      "endedAt": "2026-07-22T09:21:12.902",
                      "durationSeconds": 342,
                      "completed": true
                    }"""))),
        @ApiResponse(responseCode = "400", description = "`sessionId` was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Intended response when the session does not exist or belongs to another learner. "
                + "Currently delivered as 500 - see the tag description.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "What an unknown or foreign `sessionId` actually returns today, and any other unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/progress/sessions/{sessionId}/complete")
    public ResponseEntity<VoiceConversationSessionResponse> completeConversationTracking(
            @Parameter(description = "Identifier returned when tracking was started. Must belong to the caller.", required = true, example = "418")
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String userId = requireUserId(jwt);
        try {
            return ResponseEntity.ok(VoiceConversationSessionResponse.from(
                    progressService.complete(userId, sessionId)));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    @Operation(
        summary = "Get today's speaking progress",
        description = """
            Returns the signed-in learner's speaking practice for today measured against their daily
            targets: how many distinct characters they have spoken with, how many minutes they have
            practised, and whether both goals are met.

            Only completed sessions are counted.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Today's progress.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VoiceDailyProgressResponse.class),
                examples = @ExampleObject(name = "progress", value = """
                    {
                      "date": "2026-07-22",
                      "conversationCount": 3,
                      "uniqueCharactersCount": 2,
                      "totalSeconds": 742,
                      "totalMinutes": 12,
                      "charactersTarget": 3,
                      "minutesTarget": 15,
                      "charactersProgress": 0.67,
                      "minutesProgress": 0.8,
                      "completed": false
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/progress/daily")
    public ResponseEntity<VoiceDailyProgressResponse> dailyProgress(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(progressService.daily(requireUserId(jwt)));
    }

    private String requireUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaimAsString("sub") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return jwt.getClaimAsString("sub");
    }
}
