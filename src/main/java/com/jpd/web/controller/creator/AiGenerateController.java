package com.jpd.web.controller.creator;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.GenerateFeedbackForm;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.CreatorRequestNumber;
import com.jpd.web.repository.CreatorRequestNumberRepository;
import com.jpd.web.service.AIService;
import com.jpd.web.service.FireBaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;

@RequestMapping("/api/creator/AI")
@RestController
@Tag(name = "Creator AI tools", description = "AI assistance for authoring course material.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class  AiGenerateController {
	@Autowired
	private AIService aiService;
	@Autowired
	private CreatorRequestNumberRepository creatorRequestNumberRepository;
	@Autowired
	private FireBaseService fireBaseService;
	@Transactional
	public boolean canMakeRequest(String creatorId) {
	    Optional<CreatorRequestNumber> existing = 
	        creatorRequestNumberRepository.findByCreatorId(creatorId);
	    
	    // First request
	    if (existing.isEmpty()) {
	        CreatorRequestNumber record = CreatorRequestNumber.builder()
	            .creatorId(creatorId)
	            .number(1)
	            .lastUpdate(LocalDateTime.now())
	            .build();
	        creatorRequestNumberRepository.save(record);
	        return true;
	    }
	    
	    CreatorRequestNumber record = existing.get();
	    LocalDateTime now = LocalDateTime.now();
	    LocalDateTime today0h = now.toLocalDate().atStartOfDay();
	    
	    // Reset nếu lastUpdate < hn     ôm nay 0h
	    if (record.getLastUpdate().isBefore(today0h)) {
	        record.setNumber(1);
	        record.setLastUpdate(now);
	        creatorRequestNumberRepository.save(record);
	        return true;
	    }
	    
	    // Check limit (5 requests/day)
	    if (record.getNumber() >= 5) {
	        return false;
	    }
	    
	    // Increment
	    record.setNumber(record.getNumber() + 1);
	    creatorRequestNumberRepository.save(record);
	    return true;
	}
	@Operation(
	    summary = "Generate model feedback for an answer",
	    description = """
	        Asks the AI to write feedback on a learner's answer to a question, for the creator to reuse as
	        model feedback in their course material. The reply is returned as plain text.

	        The caller's identity is not used and nothing is persisted. Note the path is spelled
	        `generateFeeback`.

	        The class carries a `canMakeRequest` helper enforcing five requests per creator per day, but
	        this handler never calls it — **the endpoint is not currently rate-limited**.
	        """)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
	    required = true,
	    description = "The question and the learner's answer to critique.",
	    content = @Content(mediaType = "application/json",
	        schema = @Schema(implementation = GenerateFeedbackForm.class),
	        examples = @ExampleObject(name = "feedback", value = """
	            {
	              "question": "Describe what you did yesterday.",
	              "answer": "I go to school yesterday with my friend."
	            }""")))
	@ApiResponses({
	    @ApiResponse(responseCode = "201", description = "The generated feedback, as plain text.",
	        content = @Content(mediaType = "text/plain", schema = @Schema(type = "string",
	            example = "Good attempt. The verb should be past tense: 'I went to school yesterday with my friend.'"))),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "500", description = "Malformed or missing request body, or the AI call failed.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/generateFeeback")
	public ResponseEntity<?> generateFeedback( @RequestBody GenerateFeedbackForm form ) throws IllegalAccessException{
		
		String feedBack=this.aiService.generateFeedback(form.getQuestion(), form.getAnswer());
		
		return ResponseEntity.status(HttpStatus.CREATED).body(feedBack);
	}

	
}
