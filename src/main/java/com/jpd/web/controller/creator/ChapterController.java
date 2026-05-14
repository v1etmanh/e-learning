package com.jpd.web.controller.creator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.jpd.web.model.Chapter;
import com.jpd.web.service.ChapterService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/creator/{courseId}/chapter")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping
    public ResponseEntity<?> createChapter(
            @NotBlank(message = "Chapter name is required") 
            @RequestParam("chapterName") String name,
            @Positive(message = "Course ID must be positive") 
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Creating chapter '{}' for course {}", name, courseId);
        
        String creatorId = jwt.getClaimAsString("sub");
        
        // ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
        Chapter chapter = chapterService.createChapter(name, courseId, creatorId);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(chapter);
    }
    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @Positive(message = "Chapter ID must be positive") 
            @PathVariable("chapterId") Long chapterId,
            @Positive(message = "Course ID must be positive") 
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Deleting chapter {} from course {}", chapterId, courseId);
        
        String creatorId= jwt.getClaimAsString("sub");
        
        // ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
        chapterService.deleteChapter(chapterId, courseId, creatorId);
        
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{chapterID}/update")
    public ResponseEntity<?>updateChapter(@RequestParam String name ,@AuthenticationPrincipal Jwt jwt,
    		@PathVariable("chapterID")long chapterId){
  
    	String creatorId=jwt.getClaimAsString("sub");
    	this.chapterService.updateChapter(creatorId, name, chapterId);
    	return ResponseEntity.noContent().build();
    }


   
   
}