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

import com.jpd.web.dto.GenerateFeedbackForm;
import com.jpd.web.model.CreatorRequestNumber;
import com.jpd.web.repository.CreatorRequestNumberRepository;
import com.jpd.web.service.AIService;
import com.jpd.web.service.FireBaseService;

import jakarta.transaction.Transactional;

@RequestMapping("/api/creator/AI")
@RestController
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
	@PostMapping("/generateFeeback")
	public ResponseEntity<?> generateFeedback( @RequestBody GenerateFeedbackForm form ) throws IllegalAccessException{
		
		String feedBack=this.aiService.generateFeedback(form.getQuestion(), form.getAnswer());
		
		return ResponseEntity.status(HttpStatus.CREATED).body(feedBack);
	}

	
}
