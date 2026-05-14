package com.jpd.web.controller.customer;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.WritingScores;
import com.jpd.web.dto.WritingTextEvaluateForm;
import com.jpd.web.model.Language;
import com.jpd.web.model.SemanticResult;
import com.jpd.web.service.AIService;
import com.jpd.web.service.AiEvaluateService;


import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/customer/evaluate")
@Slf4j
public class   AIEvaluateController {
@Autowired
private AiEvaluateService aiEvaluateService;
@Autowired
private AIService aiService;
@PostMapping("/evaluate/{moduleId}")
public ResponseEntity<SemanticResult> evaluateAnswer(
		@RequestParam("audio") MultipartFile file,
                                                   @RequestParam("sentence") String expectedAnswer,
                                                 @PathVariable("moduleId") long moduleId,
                                                 @RequestParam(value = "language", required = false) String language,@AuthenticationPrincipal Jwt jwt) {
	 String customerId=jwt.getClaimAsString("sub");


    
	try {
        
        SemanticResult result = aiEvaluateService.evaluateSpeaking(file, expectedAnswer, language,customerId,moduleId);
        
        
        return ResponseEntity.ok(result);
        
    } catch (Exception e) {
        log.error("Lỗi khi xử lý audio: ", e);
        return ResponseEntity.badRequest().build();
    }
}
@PostMapping("/evaluateWriting")
public ResponseEntity<?> evaluateWritingText(@RequestBody WritingTextEvaluateForm form,@AuthenticationPrincipal Jwt jwt)
{ String customerId=jwt.getClaimAsString("sub");
	WritingScores score=this.aiService.evaluateWritingSimple(form.getWritingText(), form.getLanguage(),customerId);
	
	return ResponseEntity.ok(score);
}
}
