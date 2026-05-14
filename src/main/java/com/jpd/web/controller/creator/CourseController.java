package com.jpd.web.controller.creator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;

import com.jpd.web.dto.PopularCourseDTO;

import com.jpd.web.model.Course;

import com.jpd.web.service.CourseService;

import com.jpd.web.transform.CourseTransForm;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/creator/course")
public class CourseController {
	@Autowired
	private CourseService courseService;

	@PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CourseCardDto> createCourse(@Valid @ModelAttribute CourseFormDto entity,
													  @AuthenticationPrincipal Jwt jwt) {
		// TODO: process POST request
		Course c;

		String creatorId = jwt.getClaimAsString("sub");

			c = this.courseService.createCourse(entity, creatorId);
			return ResponseEntity.status(HttpStatus.CREATED).body(CourseTransForm.transformToCourseCardDto(c));
		

	}

	@GetMapping()
	public ResponseEntity<List<CourseCardDto>> retrieveAll(@AuthenticationPrincipal Jwt jwt) {
		
			String creatorId = jwt.getClaimAsString("sub");
			List<CourseCardDto> courses = this.courseService.retrieveCourseByemail(creatorId);
			return ResponseEntity.status(HttpStatus.OK).body(courses);
		
	}

	@GetMapping("/{id}")
	public ResponseEntity<CourseContentDto> retrieveCourseById(
			 @Positive(message = "Course ID must be positive") 
			@PathVariable("id") long id, @AuthenticationPrincipal Jwt jwt ) {

		String creatorId = jwt.getClaimAsString("sub");
			return ResponseEntity.status(HttpStatus.OK).body(this.courseService.getCourseById(id, creatorId));
		

	}

	public CourseController() {
		// TODO Auto-generated constructor stub
	}

	
	@GetMapping("/{id}/setCourseStatus")
	public ResponseEntity<?> putMethodName(@PathVariable long id,@AuthenticationPrincipal Jwt jwt) {
		//TODO: process PUT request
		String creatorId = jwt.getClaimAsString("sub");
		
		this.courseService.changeCourseSatus(id, creatorId);
		return ResponseEntity.status(HttpStatus.OK).build();
	}
	@GetMapping("/retrieve_CommercialCourese")
	public ResponseEntity<List<PopularCourseDTO>>retrieveCCourse(@AuthenticationPrincipal Jwt jwt) {
		String creatorId = jwt.getClaimAsString("sub");
		List<PopularCourseDTO>cpp=this.courseService.retrieveCCourse(creatorId);
		return ResponseEntity.ok(cpp);
	}


}
