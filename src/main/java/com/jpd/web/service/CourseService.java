package com.jpd.web.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;

import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.exception.ApiException;
import com.jpd.web.exception.UnauthorizedException;

import com.jpd.web.service.utils.CodeGenerator;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;
import com.jpd.web.transform.CreatorTransform;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CourseService {
	@Autowired
	private FireBaseService fireBaseService;

	@Autowired
	private CourseRepository courseRepository;
    @Autowired
	private ModuleContentRepository moduleContentRepository;
	@Autowired
	private CreatorRepository creatorRepository;
	@Autowired
	private ValidationResources resourceValidator;
	@Autowired
	private CodeGenerator codeGenerator;

   @Autowired
   private CourseMetricsRepository courseMetricsRepository;

	private String uploadCourseImage(MultipartFile imgFile) {
		try {
			String imgUrl = fireBaseService.uploadFile(imgFile, TypeOfFile.IMG);

			if (imgUrl == null || imgUrl.trim().isEmpty()) {
				throw new FileUploadException("Failed to upload course image");
			}

			return imgUrl;
		} catch (Exception e) {
			log.error("Error uploading course image", e);
			throw new ApiException("Failed to upload course image: " + e.getMessage());
		}
	}

	@Transactional
	public Course createCourse(CourseFormDto courseFormDto, String creatorId) {
		log.info("Creating course '{}' for creator {}", courseFormDto.getName(), creatorId);

		// Validate creator exists


		// Transform DTO to entity
		Course course = CourseTransForm.transformFromCourseFormDto(courseFormDto);

		// Validate paid course requirements


		// Upload course image
		String imgUrl = uploadCourseImage(courseFormDto.getImgFile());
		course.setUrlImg(imgUrl);
        Creator creator=this.creatorRepository.findById(creatorId).get();
		// Set creator
		course.setCreator(creator);

		// Generate join key for private courses
		//
			course.setJoinKey(codeGenerator.generate6DigitCode());
		
       
		Course savedCourse = courseRepository.save(course);
		CourseMetrics metrics = CourseMetrics.builder()
				.course(savedCourse)
				.instructorName(creator.getFullName())
				.totalStudents(0)
				.totalFeedbacks(0)
				.averageRating(0.0)
				.totalRating(0)

				.totalPeopleLike(0)
				.totalReport(0)
				.build();
		courseMetricsRepository.save(metrics);
		log.info("Successfully created course {} for creator {}", savedCourse.getCourseId(), creatorId);

		return savedCourse;
	}

	public List<CourseCardDto> retrieveCourseByemail(String creatorId)  {
		Optional<Creator> c = this.creatorRepository.findById(creatorId);

		List<Course> courses = c.get().getCourses();
		return courses.stream().map(e -> CourseTransForm.transformToCourseCardDto(e)).collect(Collectors.toList());
	}

	@Transactional
	public CourseContentDto getCourseById(long courseId, String creatorId)  {

		log.info("Retrieving course {} for creator {}", courseId, creatorId);

		// Validate course exists and creator owns it
		Course course = resourceValidator.validateCourseOwnership(courseId, creatorId);

		course.getChapters().forEach(chapter -> {
			chapter.getModules().forEach(module -> {
				module.setContentTypes(this.moduleContentRepository.findTypeOfContentByModuleId(module.getModuleId()));
			});


		});
		CourseContentDto cdto = CourseTransForm.transformToCourseContentDto(course);
		return cdto;
	}

  public void changeCourseSatus(long courseId, String creatorId) {
	  Course c=this.resourceValidator.validateCourseOwnership(courseId, creatorId);
	  c.setPublic(!c.isPublic());
	  courseRepository.save(c);
	  return;
  }

	public List<PopularCourseDTO > retrieveCCourse(String creatorId){
		Creator creator=this.creatorRepository.findById(creatorId).get();
		List<Course> paidCourses = creator.getCourses().stream()

				.toList();
		List<PopularCourseDTO>ppc=paidCourses.stream().map(e->CreatorTransform.transform(e)).collect(Collectors.toList());
		return ppc;
	}
}
