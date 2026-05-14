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

import com.jpd.web.model.ModuleContent;
import com.jpd.web.service.KahootModuleContentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/creator/kahootModuleContent/{kahootId}")
public class KahootModuleContentController {
@Autowired
private KahootModuleContentService kahootModuleContentService;
@DeleteMapping("/{moduleContentId}")
public ResponseEntity<?> deleteModuleContent(@Positive @PathVariable("moduleContentId") long moduleContentId,
                                             @Positive @PathVariable("kahootId") Long kahootId,

                                             @AuthenticationPrincipal Jwt jwt) {

    String creatorId = jwt.getClaimAsString("sub");
    kahootModuleContentService.deleteModuleContent(
    		moduleContentId, kahootId, creatorId
    );
    
    return ResponseEntity.noContent().build();
  
}

@PostMapping
public ResponseEntity<?> updateModuleContents(
        @Valid @RequestBody List<ModuleContent>mds,
        @Positive @PathVariable("kahootId") Long kahootId,
        @AuthenticationPrincipal Jwt jwt) {

    String creatorId = jwt.getClaimAsString("sub");
    List<ModuleContent> contents = this.kahootModuleContentService.updateCourseMaterial(mds,kahootId, creatorId);
    return ResponseEntity.ok(contents);
}
}
