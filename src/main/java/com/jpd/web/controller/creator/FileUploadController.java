package com.jpd.web.controller.creator;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.model.TypeOfFile;
import com.jpd.web.service.FileUploadService;

@RestController
@RequestMapping("/api/creator/uploadFile")
public class FileUploadController {
	@Autowired
	private FileUploadService fileUploadService;
	@PostMapping("/savePdf")
	 public ResponseEntity<?> storePDf(  @RequestParam("pdf") MultipartFile pdf,
			 @AuthenticationPrincipal Jwt jwt) throws IllegalAccessException, IOException {
	
	 	String creatorId=jwt.getClaimAsString("sub");
	    String a=this.fileUploadService.saveImgIntoFirebase(creatorId,pdf,TypeOfFile.PDF);
	    if(a==null) {return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();}
	    else return ResponseEntity.status(HttpStatus.OK).body(a);
	
	}
	@PostMapping("/saveImg")
	public ResponseEntity<?> postMethodName(  @RequestParam("img") MultipartFile img,
											  @AuthenticationPrincipal Jwt jwt) throws IllegalAccessException, IOException {

		String creatorId=jwt.getClaimAsString("sub");
	   String a=this.fileUploadService.saveImgIntoFirebase(creatorId,img,TypeOfFile.IMG);
	   if(a==null) {return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();}
	   else return ResponseEntity.status(HttpStatus.OK).body(a);
	
	}
	@DeleteMapping("/delete_file")
	public ResponseEntity<?> deleteImage(@RequestParam("url")String url, @AuthenticationPrincipal Jwt jwt)
	{
		String creatorId=jwt.getClaimAsString("sub");
		 this.fileUploadService.deleteFileByUrl(url, creatorId);
		 return ResponseEntity.noContent().build();
	}
	
}
