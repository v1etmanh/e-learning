package com.jpd.web.controller.customer;
import com.jpd.web.dto.*;
import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.model.JoinSessionRequest;
import com.jpd.web.model.SessionInfo;
import com.jpd.web.service.SessionService;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
@Tag(name = "Live quiz", description = """
        Kahoot-style live quiz sessions, held in Redis. The host creates a session and drives it question
        by question; learners join with a six-digit PIN.

        `POST /api/quiz/join` is public — learners do not need an account. Every other endpoint here
        requires a bearer token, but none of them checks that the caller is the session's host: any
        authenticated user who knows a session code can read its state, delete it, or close its current
        question.

        Most handlers catch every exception internally, so failures collapse into 400 with either a
        `{ "success": false, "message": ... }` body or no body at all — the global error shape is not used.
        Real-time gameplay runs over STOMP (`/ws-quiz`), which is outside this document.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class QuizSessionController {

    @Autowired
    private SessionService sessionService;

    /**
     * Tạo session mới
     */
    @Operation(
        summary = "Create a live quiz session",
        description = """
            Opens a new session from one of the caller's Kahoots and returns the PIN, join URL and QR
            code to share with learners. The session starts in `WAITING`.

            The Kahoot must belong to the caller and must contain at least one `MULTIPLE_CHOICE` or
            `GAPFILL` question — other content types cannot be played.

            Every failure is caught and returned as **400 with an empty body**: not owning the Kahoot, the
            Kahoot having no playable questions, and Redis being unavailable are indistinguishable to the caller.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = CreateSessionRequest.class),
            examples = @ExampleObject(name = "create", value = """
                {
                  "kahootId": 77,
                  "teacherName": "Co Thuy"
                }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session created and waiting for learners.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CreateSessionResponse.class),
                examples = @ExampleObject(name = "created", value = """
                    {
                      "sessionCode": "482913",
                      "sessionId": "3f1a9c22-77bd-4e0a-9b41-5c2d8e6f0a11",
                      "qrCodeUrl": "https://api.qrserver.com/v1/create-qr-code/?data=http://localhost:3000/quiz/join/482913",
                      "joinUrl": "http://localhost:3000/quiz/join/482913",
                      "title": "On tap Kanji N5",
                      "totalQuestions": 15
                    }"""))),
        @ApiResponse(responseCode = "400", description = "The Kahoot does not belong to the caller, has no playable questions, or could not be stored. Empty body.",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content)
    })
    @PostMapping("/create")
    public ResponseEntity<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest request,
                                                               @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        try {
            String customerId = jwt.getClaimAsString("sub");
    		
            CreateSessionResponse response = sessionService.createSession(request,customerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Join session (REST endpoint - dùng để validate trước khi connect WebSocket)
     */
    @Operation(
        summary = "Join a live quiz session",
        description = """
            Registers a learner in a waiting session and returns their participant record together with
            the full session state. Call this before opening the STOMP connection — it is the validation
            step for the WebSocket handshake.

            **Public endpoint — no bearer token required.** Anyone with the PIN can join under any nickname.

            Joining is only possible while the session is `WAITING`; `ACTIVE` and `FINISHED` sessions are
            refused. All failures return 400 with `success: false` and a `message`, never the standard
            error shape.
            """)
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = JoinSessionRequest.class),
            examples = @ExampleObject(name = "join", value = """
                {
                  "sessionCode": "482913",
                  "participantName": "Thuy"
                }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Joined. The body carries the participant record and the session state.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "joined", value = """
                    {
                      "success": true,
                      "participant": {
                        "participantId": "b7d1e0c4-3a92-4f55-8c10-2e6f9a0b1c33",
                        "name": "Thuy",
                        "sessionCode": "482913",
                        "joinedAt": "2026-07-22T09:15:30.412",
                        "currentScore": 0
                      },
                      "session": {
                        "sessionCode": "482913",
                        "title": "On tap Kanji N5",
                        "status": "WAITING",
                        "totalQuestions": 15,
                        "totalParticipants": 24
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "No session with that code, the quiz has already started or finished, or joining failed.",
            content = @Content(mediaType = "application/json",
                examples = {
                    @ExampleObject(name = "notFound", value = """
                        { "success": false, "message": "Session not found" }"""),
                    @ExampleObject(name = "alreadyStarted", value = """
                        { "success": false, "message": "Quiz already started" }"""),
                    @ExampleObject(name = "finished", value = """
                        { "success": false, "message": "Quiz has already finished" }""")}))
    })
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinSession(@RequestBody JoinSessionRequest request) {
        try {
            // Validate session exists
            SessionInfo session = sessionService.getSession(request.getSessionCode());
            if (session == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Session not found"
                ));
            }
            
            // Validate session status
            if (session.getStatus() == com.jpd.web.model.SessionStatus.FINISHED) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Quiz has already finished"
                ));
            }
            
            if (session.getStatus() == com.jpd.web.model.SessionStatus.ACTIVE) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Quiz already started"
                ));
            }
            
            // Join session
            ParticipantInfo participant = sessionService.joinSession(request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "participant", participant,
                "session", session
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get session info
     */
    @Operation(
        summary = "Get a session's current state",
        description = """
            Returns the full session record: status, participant counts, the question currently in play
            and its timing.

            Any authenticated caller who knows the session code can read this — the host is not verified.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current session state.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionInfo.class))),
        @ApiResponse(responseCode = "400", description = "The session could not be read from Redis. Empty body.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No session exists with this code. Empty body.", content = @Content)
    })
    @GetMapping("/{sessionCode}")
    public ResponseEntity<SessionInfo> getSession(
            @Parameter(description = "Six-digit PIN of the session.", required = true, example = "482913")
            @PathVariable String sessionCode) {
        try {
            SessionInfo session = sessionService.getSession(sessionCode);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Get all participants
     */
    @Operation(
        summary = "List a session's participants",
        description = """
            Returns everyone who has joined the session, with their current scores — the data behind the
            lobby list and the leaderboard.

            Any authenticated caller who knows the session code can read this; the host is not verified.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participants in the session. Empty when nobody has joined, and also when the session does not exist.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ParticipantInfo.class)),
                examples = @ExampleObject(name = "participants", value = """
                    [
                      {
                        "participantId": "b7d1e0c4-3a92-4f55-8c10-2e6f9a0b1c33",
                        "name": "Thuy",
                        "sessionCode": "482913",
                        "joinedAt": "2026-07-22T09:15:30.412",
                        "currentScore": 1750
                      }
                    ]"""))),
        @ApiResponse(responseCode = "400", description = "The participant list could not be read from Redis. Empty body.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content)
    })
    @GetMapping("/{sessionCode}/participants")
    public ResponseEntity<List<ParticipantInfo>> getParticipants(
            @Parameter(description = "Six-digit PIN of the session.", required = true, example = "482913")
            @PathVariable String sessionCode) {
        try {
            List<ParticipantInfo> participants = sessionService.getParticipants(sessionCode);
            return ResponseEntity.ok(participants);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Delete session
     */
    @Operation(
        summary = "Delete a session",
        description = """
            Removes the session and its participants from Redis. Irreversible — scores are not persisted
            anywhere else.

            **No ownership check.** Any authenticated caller who knows the session code can delete
            someone else's running quiz.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session deleted.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "deleted", value = """
                    { "success": true, "message": "Session deleted" }"""))),
        @ApiResponse(responseCode = "400", description = "The session could not be deleted.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "failed", value = """
                    { "success": false, "message": "Failed to delete session: connection refused" }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content)
    })
    @DeleteMapping("/{sessionCode}")
    public ResponseEntity<Map<String, Object>> deleteSession(
            @Parameter(description = "Six-digit PIN of the session to delete.", required = true, example = "482913")
            @PathVariable String sessionCode) {
        try {
            sessionService.deleteSession(sessionCode);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session deleted"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    @Operation(
        summary = "Submit an answer to the open question",
        description = """
            Records a participant's answer to the question currently in play and returns how many of the
            participants have now answered.

            Whether the answer was correct is deliberately not revealed here — results are disclosed when
            the host closes the question.

            The answer is rejected if the session is not accepting answers or the question's time limit
            has already passed. Unlike the other handlers in this controller, this one does not swallow
            its exceptions, so those rejections surface as 500 through the global handler.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = SubmitAnswerRequest.class),
            examples = @ExampleObject(name = "answer", value = """
                {
                  "sessionCode": "482913",
                  "participantId": "b7d1e0c4-3a92-4f55-8c10-2e6f9a0b1c33",
                  "questionId": 9042,
                  "answer": "B"
                }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Answer accepted.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SubmitAnswerResponse.class),
                examples = @ExampleObject(name = "accepted", value = """
                    {
                      "success": true,
                      "message": "Answer submitted",
                      "totalAnswered": 18,
                      "totalParticipants": 24
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No such session or participant, answers are not being accepted, or the time limit has passed.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.jpd.web.exception.ErrorResponse.class)))
    })
    @PostMapping("/submit-answer")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.ok(sessionService.submitAnswer(request));
    }

    @Operation(
        summary = "Close the open question and reveal results",
        description = """
            Stops accepting answers for the question in play, scores every submission, and returns the
            per-participant results together with the correct answer.

            Driven by the host between questions. **No ownership check** — any authenticated caller who
            knows the session code can close a question. Failures surface as 500 through the global handler.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Results for the closed question.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QuestionResultResponse.class),
                examples = @ExampleObject(name = "results", value = """
                    {
                      "questionId": 9042,
                      "correctAnswer": "B",
                      "results": [
                        {
                          "participantId": "b7d1e0c4-3a92-4f55-8c10-2e6f9a0b1c33",
                          "participantName": "Thuy",
                          "answer": "B",
                          "correct": true,
                          "points": 850,
                          "totalScore": 1750
                        }
                      ]
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No session with this code, or no question is currently open.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.jpd.web.exception.ErrorResponse.class)))
    })
    @PostMapping("/end-question/{sessionCode}")
    public ResponseEntity<QuestionResultResponse> endQuestion(
            @Parameter(description = "Six-digit PIN of the session.", required = true, example = "482913")
            @PathVariable String sessionCode) {
        return ResponseEntity.ok(sessionService.endQuestion(sessionCode));
    }
}