package com.jpd.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.dto.LearningListDto;
import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/customer")
public class CustomerController {
@Autowired
private CustomerService customerSer;
@PostMapping(value="/upload_profile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<CreatorDto> updateProfile( @Valid @ModelAttribute CreatorProfileDto creatorProfileDto,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
	String email=jwt.getClaimAsString("email");
	String customerId=jwt.getClaimAsString("sub");
 CreatorDto crdto=   this.customerSer.uploadProfile(email,customerId, creatorProfileDto);
  return ResponseEntity.status(HttpStatus.CREATED).body(crdto);
 
    
    
}
@GetMapping("/learning_course_list")
public ResponseEntity<LearningListDto> retrieveYourCourses(@AuthenticationPrincipal
		Jwt jwt){
	String customerId=jwt.getClaimAsString("sub");
	LearningListDto res=this.customerSer.retrieveLearningList(customerId);
	return ResponseEntity.status(HttpStatus.OK).body(res);
}

}
