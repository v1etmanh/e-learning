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

import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.service.ModuleContentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/api/creator/{courseId}/{chapterId}/{moduleId}")
@Slf4j
public class ModuleContentController {
    @Autowired
    private ModuleContentService moduleContentService;
    @GetMapping()
    public ResponseEntity<List<ModuleContent>> getModuleContentByTypeAndModuleId(@RequestParam("type") TypeOfContent typeOfContent,
                                                                                 @Positive @PathVariable("moduleId") Long moduleId,
                                                                                 @Positive @PathVariable("chapterId") Long chapterId,
                                                                                 @Positive @PathVariable("courseId") Long courseId,
                                                                                 @AuthenticationPrincipal Jwt jwt) {
        //return
        String creatorId=jwt.getClaimAsString("sub");
        List<ModuleContent>mds=moduleContentService.getModuleContentsByType(typeOfContent,moduleId,creatorId);

        return ResponseEntity.ok().body(mds);
    }
    @DeleteMapping("/{moduleContentId}")
    public ResponseEntity<?> deleteModuleContent(    @Positive @PathVariable("moduleContentId") long moduleContentId,
                                                     @Positive @PathVariable("moduleId") Long moduleId,
                                                     @Positive @PathVariable("chapterId") Long chapterId,
                                                     @Positive @PathVariable("courseId") Long courseId,
                                                     @AuthenticationPrincipal Jwt jwt               ) {

        String creatorId=jwt.getClaimAsString("sub");
        moduleContentService.deleteModuleContent(
                moduleContentId, moduleId, creatorId
        );

        return ResponseEntity.ok().build();

    }
    @DeleteMapping("/deleteModuleContentByType")
    public ResponseEntity<?> deleteModuleContentByType(@RequestParam("type") TypeOfContent type,
                                                       @Positive    @PathVariable ("moduleId")long moduleId,
                                                       @Positive     @PathVariable ("chapterId")long chapterId,
                                                       @Positive    @PathVariable ("courseId")long courseId,
                                                       @AuthenticationPrincipal Jwt jwt
    ) {
        String creatorId=jwt.getClaimAsString("sub");
        moduleContentService.deleteModuleContentsByType( type, moduleId, creatorId);
        return ResponseEntity.noContent().build();

    }
    @PostMapping
    public ResponseEntity<?> updateModuleContents(
            @Valid @RequestBody ModuleContentDto moduleContentDto,
            @AuthenticationPrincipal Jwt jwt) {

        String creatorId=jwt.getClaimAsString("sub");
         var result = moduleContentService.updateCourseMaterial(moduleContentDto, creatorId);
        return ResponseEntity.ok(result);
    }
}