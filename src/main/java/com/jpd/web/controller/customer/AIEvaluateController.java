package com.jpd.web.controller.customer;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import com.jpd.web.dto.WritingScores;
import com.jpd.web.dto.WritingTextEvaluateForm;
import com.jpd.web.dto.IeltsBrainstormForm;
import com.jpd.web.dto.MagicDiaryEvaluateForm;
import com.jpd.web.dto.MagicDiaryScores;
import com.jpd.web.dto.MagicDiaryQuestionForm;
import com.jpd.web.dto.MagicDiaryQuestionResponse;
import com.jpd.web.model.Language;
import com.jpd.web.model.SemanticResult;
import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.AIService;
import com.jpd.web.service.AiEvaluateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/customer/evaluate")
@Slf4j
@Tag(name = "AI evaluation", description = """
        AI-assisted practice: speaking assessment, writing scoring, the Magic Diary journal and IELTS
        brainstorming. Backed by Gemini.

        Speaking and Magic Diary evaluation are each rate-limited to 10 calls per customer per day.
        The writing, Magic Diary and question endpoints never surface an AI failure as an HTTP error —
        they return a neutral fallback body with 200 instead.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class   AIEvaluateController {
@Autowired
private AiEvaluateService aiEvaluateService;
@Autowired
private AIService aiService;
@Operation(
    summary = "Evaluate spoken audio against an expected sentence",
    description = """
        Transcribes an uploaded audio recording and compares it semantically with the sentence the
        learner was asked to say, returning a similarity score and pronunciation feedback.

        Rate-limited to 10 evaluations per customer per day.

        Every failure inside this handler is caught and flattened to **400 with an empty body** — the
        daily limit, an empty audio file, a blank `sentence`, a missing `language`, and any transcription
        or scoring error are all indistinguishable to the caller. There is no error message.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Evaluation of the recording.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = SemanticResult.class),
            examples = @ExampleObject(name = "match", value = """
                {
                  "match": true,
                  "similarity_score": 0.92,
                  "user_answer": "今日はいい天気ですね",
                  "expected_answer": "今日はいい天気ですね",
                  "feedback": "Phát âm rõ ràng. Chú ý kéo dài âm ở cuối câu khi dùng ね.",
                  "has_error": false,
                  "error_message": null
                }"""))),
    @ApiResponse(responseCode = "400", description = "Empty audio file, blank `sentence`, missing `language`, the daily limit of 10 "
            + "evaluations was reached, or the evaluation pipeline failed. Empty body — the cause is not reported.",
        content = @Content),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content)
})
@PostMapping("/evaluate/{moduleId}")
public ResponseEntity<SemanticResult> evaluateAnswer(
		@io.swagger.v3.oas.annotations.Parameter(description = "The learner's recording.", required = true,
				schema = @Schema(type = "string", format = "binary"))
		@RequestParam("audio") MultipartFile file,
                                                   @io.swagger.v3.oas.annotations.Parameter(description = "The sentence the learner was asked to say. Must not be blank.",
                                                		   required = true, example = "今日はいい天気ですね")
                                                   @RequestParam("sentence") String expectedAnswer,
                                                 @io.swagger.v3.oas.annotations.Parameter(description = "Identifier of the module this practice belongs to. Recorded with the result.",
                                                		 required = true, example = "56")
                                                 @PathVariable("moduleId") long moduleId,
                                                 @io.swagger.v3.oas.annotations.Parameter(description = "Language spoken in the recording. Declared optional, but the service rejects the request when it is absent.",
                                                		 example = "JAPANESE")
                                                 @RequestParam(value = "language", required = false) String language,
                                                 @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
	 String customerId=jwt.getClaimAsString("sub");


    
	try {
        
        SemanticResult result = aiEvaluateService.evaluateSpeaking(file, expectedAnswer, language,customerId,moduleId);
        
        
        return ResponseEntity.ok(result);
        
    } catch (Exception e) {
        log.error("Lỗi khi xử lý audio: ", e);
        return ResponseEntity.badRequest().build();
    }
}
@Operation(
    summary = "Score a piece of free writing",
    description = """
        Sends the submitted text to Gemini for grammar and vocabulary scoring, saves the result against
        the signed-in customer, and returns the scores with written feedback.

        Never fails on the AI: if the model call or its response parsing breaks, the endpoint still
        returns 200 with a neutral fallback — `grammar` and `vocabulary` both `5.0` and the feedback
        "Could not evaluate. Please try again." Not rate-limited.
        """)
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    required = true,
    description = "The text to score and the language it is written in.",
    content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = WritingTextEvaluateForm.class),
        examples = @ExampleObject(name = "writing", value = """
            {
              "writingText": "Yesterday I went to the library and borrowed three books about Japanese history.",
              "language": "ENGLISH"
            }""")))
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Scores and feedback, or the neutral fallback if the AI call failed.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WritingScores.class),
            examples = {
                @ExampleObject(name = "scored", value = """
                    {
                      "grammar": 7.5,
                      "vocabulary": 8.0,
                      "feedback": "Good range of vocabulary. Watch your use of past tense in the second paragraph."
                    }"""),
                @ExampleObject(name = "aiFallback", value = """
                    {
                      "grammar": 5.0,
                      "vocabulary": 5.0,
                      "feedback": "Could not evaluate. Please try again."
                    }""")})),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "500", description = "Malformed or missing request body.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/evaluateWriting")
public ResponseEntity<?> evaluateWritingText(@RequestBody WritingTextEvaluateForm form,@io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)
{ String customerId=jwt.getClaimAsString("sub");
	WritingScores score=this.aiService.evaluateWritingSimple(form.getWritingText(), form.getLanguage(),customerId);
	
	return ResponseEntity.ok(score);
}

@Operation(
    summary = "Grade a Magic Diary entry against its lesson",
    description = """
        Grades a journal entry the learner wrote about a lesson. Unlike plain writing evaluation, this
        also scores `contentAccuracy` — how well the entry matches the facts supplied in `referenceNotes`
        — and suggests a concrete next step.

        Rate-limited to 10 evaluations per customer per day; exceeding it returns 400.

        Beyond the limit check, AI failures are not surfaced: a broken model call still returns 200 with
        all three scores at `5.0` and the feedback "Could not evaluate. Please try again."
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Grading of the entry, or the neutral fallback if the AI call failed.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = MagicDiaryScores.class),
            examples = @ExampleObject(name = "graded", value = """
                {
                  "contentAccuracy": 8.5,
                  "grammar": 7.0,
                  "vocabulary": 7.5,
                  "feedback": "Bạn nắm đúng mốc thời gian 1603-1868. Câu thứ hai nên dùng thể quá khứ.",
                  "nextStep": "Viết thêm một đoạn về chính sách sakoku và ảnh hưởng của nó."
                }"""))),
    @ApiResponse(responseCode = "400", description = "A required field was blank (`code: VALIDATION_ERROR`), or the daily limit of 10 evaluations was reached.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "500", description = "Malformed or missing request body.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/magic-diary")
public ResponseEntity<MagicDiaryScores> evaluateMagicDiary(
        @Valid @RequestBody MagicDiaryEvaluateForm form,
        @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    String customerId = jwt.getClaimAsString("sub");
    return ResponseEntity.ok(aiService.evaluateMagicDiary(
            form.getWritingText(),
            form.getLanguage(),
            form.getLessonTitle(),
            form.getReferenceNotes(),
            customerId));
}

@Operation(
    summary = "Ask a question about the lesson being journalled",
    description = """
        Answers a learner's question about a lesson, using only the facts in `referenceNotes` as source
        material. Questions the notes cannot answer come back with `status: OUT_OF_SCOPE` rather than an
        invented answer.

        Not rate-limited and nothing is persisted. AI failures are reported in the body as
        `status: ERROR` with 200 — never as an HTTP error.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The answer, or a status explaining why there is none.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = MagicDiaryQuestionResponse.class),
            examples = @ExampleObject(name = "answered", value = """
                {
                  "status": "ANSWERED",
                  "answer": "Mạc phủ Tokugawa áp dụng sakoku để hạn chế ảnh hưởng của phương Tây và giữ ổn định chính trị trong nước.",
                  "correction": "",
                  "suggestedQuestion": "Chính sách sakoku kết thúc như thế nào?"
                }"""))),
    @ApiResponse(responseCode = "400", description = "A required field was blank (`code: VALIDATION_ERROR`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "500", description = "Malformed or missing request body.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/magic-diary/question")
public ResponseEntity<MagicDiaryQuestionResponse> askMagicDiaryQuestion(
        @Valid @RequestBody MagicDiaryQuestionForm form,
        @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    String customerId = jwt.getClaimAsString("sub");
    return ResponseEntity.ok(aiService.answerMagicDiaryQuestion(
            form.getQuestion(),
            form.getLessonTitle(),
            form.getReferenceNotes(),
            customerId));
}

@Operation(
    summary = "Brainstorm ideas for an IELTS essay prompt",
    description = """
        Sends the prompt to Gemini as-is — nothing is templated around it — and returns the raw reply
        wrapped in a single-field JSON object.

        Not rate-limited and nothing is persisted; the caller's identity is not used at all.
        """)
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    required = true,
    description = "The essay prompt to brainstorm.",
    content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = IeltsBrainstormForm.class),
        examples = @ExampleObject(name = "prompt", value = """
            {
              "prompt": "Some people believe that university education should be free for everyone. To what extent do you agree or disagree?"
            }""")))
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The model's reply under a `feedback` key.",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "brainstorm", value = """
                {
                  "feedback": "Agree side: widens access regardless of income; raises national skill level. Disagree side: high tax burden; devalues degrees if demand outstrips places. Suggested structure: introduction stating a partial-agreement position, one body paragraph per side, conclusion favouring means-tested funding."
                }"""))),
    @ApiResponse(responseCode = "400", description = "`prompt` was blank (`code: VALIDATION_ERROR`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "500", description = "Malformed or missing request body, or the Gemini call failed — unlike the other AI endpoints, this one has no fallback.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/ielts-brainstorm")
public ResponseEntity<?> evaluateIeltsBrainstorm(
        @Valid @RequestBody IeltsBrainstormForm form,
        @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    // Optionally log or store who requested this using jwt
    String response = aiService.evaluateIeltsBrainstorm(form.getPrompt());
    // Wrap in a simple JSON object
    return ResponseEntity.ok(java.util.Map.of("feedback", response));
}
}
