package com.jpd.web.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.jpd.web.dto.EnrollmentDto;
import com.jpd.web.model.CourseMetrics;
import com.jpd.web.repository.CourseMetricsRepository;
import com.jpd.web.service.utils.CourseMetricsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.EnrollmentExistException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;

@Service
public class EnrollmentService {
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private CourseRepository courseRepository;
	@Autowired
	private EnrollmentRepository enrollmentRepository;

	 @Autowired
	 private CourseMetricsHelper courseMetricsHelper;
	public List<Enrollment> findByCourseId(long courseId, String creatorId) {
		Course c = validationResources.validateCourseExists(courseId);
		if (!c.getCreator().getCreatorId().equals(creatorId)) {
			throw new CourseNotFoundException(courseId);
		}
		List<Enrollment> ers = this.enrollmentRepository.findByCourse(c);
		ers.forEach(e -> e.getFeedback());
		return ers;
	}

	public boolean handleEnrollCouse(long courseId, String code, String customerId) {


		Optional<Course> course = this.courseRepository.findById(courseId);
		if (course.isEmpty()) {
			throw new CourseNotFoundException(courseId);
		}
		Optional<Enrollment> e1 = this.enrollmentRepository.findByCourse_CourseIdAndCustomerId(courseId, customerId);
		if (e1.isPresent()) {
			throw new EnrollmentExistException("you have been enrroll");
		} else if (course.get().getAccessMode() == AccessMode.PRIVATE) {
			if (!course.get().getJoinKey().equals(code)) {
				return false;
			}
		}
		Enrollment e = Enrollment.builder()
				.course(course.get())
				.customerId(customerId)
				.build();
		this.enrollmentRepository.save(e);
     this.courseMetricsHelper.incrementEnrollmentCount(courseId);
		return true;


	}



}