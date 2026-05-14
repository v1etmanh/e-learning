package com.jpd.web.controller.creator;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.service.CreatorService;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@RestController
@Slf4j
@RequestMapping("api/creator/")
public class CreatorController {
@Autowired
private CreatorService creatorService;

@GetMapping("/getAccount")
public ResponseEntity<CreatorDto> getAccount(@AuthenticationPrincipal Jwt jwt) {
    String creatorId = jwt.getClaimAsString("sub");
    CreatorDto c=this.creatorService.getAccount(creatorId);
    return ResponseEntity.status(HttpStatus.OK).body(c);
}
@PostMapping("/upade_certificate") // Nên sửa thành /update_certificate
public ResponseEntity<?> updateCertificate(
        @RequestParam("certificateFile") MultipartFile[] files, // Đổi thành array
        @AuthenticationPrincipal Jwt jwt) throws FileUploadException {


    String creatorId = jwt.getClaimAsString("sub");
    log.info("Uploading {} certificates for creator {}", files.length, creatorId);
    
    // Validate và upload từng file
    for (MultipartFile file : files) {
        if (file.isEmpty()) {
            continue;
        }
        this.creatorService.upLoadCertificate(creatorId, file);
    }
    
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
    @DeleteMapping("/removeCertificate")
    public ResponseEntity<?> removeCertificate(
           @RequestParam("certificateUrl")String certificateUrl,  // Nhận từ body
            @AuthenticationPrincipal Jwt jwt) {

        String creatorId = jwt.getClaimAsString("sub");


        log.info("Removing certificate for creator {}: {}", creatorId, certificateUrl);

        this.creatorService.removeCertificate(creatorId, certificateUrl);

        return ResponseEntity.status(HttpStatus.OK).body("Certificate removed successfully");
    }
}
