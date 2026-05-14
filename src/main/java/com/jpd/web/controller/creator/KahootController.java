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

import com.jpd.web.dto.KahootDto;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.service.KahootService;
import com.jpd.web.transform.KahootTransform;

import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/api/creator/kahoot")
public class KahootController {
	@Autowired
	private KahootService kahootService;
@GetMapping("/retrieveAll")
public ResponseEntity<List<KahootDto>> retrieveAllByCreatorId(@AuthenticationPrincipal Jwt jwt){
	String creatorId=jwt.getClaimAsString("sub");
	List<KahootDto> khs=this.kahootService.retrieveAll(creatorId);
	return ResponseEntity.ok(khs);
}
@GetMapping("/{kahootId}/moduleContents")
public ResponseEntity<List<ModuleContent>> getDataOfKahoot(@PathVariable("kahootId")long kahootId,@AuthenticationPrincipal Jwt jwt)
{   String creatorId=jwt.getClaimAsString("sub");
List<ModuleContent> khs=this.kahootService.retrieveData(creatorId,kahootId);
return ResponseEntity.ok(khs);
	}
@PostMapping("/create")
public ResponseEntity<?>createKahoot(@RequestParam("title")String name,@AuthenticationPrincipal Jwt jwt)
{  String creatorId=jwt.getClaimAsString("sub");
	KahootDto k=KahootTransform.transformToKahootDto(this.kahootService.createKahoot(name, creatorId));
	return ResponseEntity.status(HttpStatus.CREATED).body(k);
	}
@DeleteMapping("/{kahootId}")
public ResponseEntity<?>deleteKahoot(@PathVariable("kahootId")long kahootId,@AuthenticationPrincipal Jwt jwt)
{  String creatorId=jwt.getClaimAsString("sub");
   this.kahootService.deleteKahoot(creatorId, kahootId);
   return ResponseEntity.noContent().build();
	}
@PutMapping("/{kahootId}")
public ResponseEntity<?> putMethodName(@PathVariable long kahootId,@RequestParam("newTitle")String t,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process PUT request
	String creatorId=jwt.getClaimAsString("sub");
	   
    this.kahootService.updateKahootTitle(creatorId, kahootId, t);
    return ResponseEntity.noContent().build();
}
}
