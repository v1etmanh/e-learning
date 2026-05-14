// java
package com.jpd.web.service.utils;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.jpd.web.exception.ChapterNotBelongsToCourseException;
import com.jpd.web.exception.ChapterNotFoundException;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.exception.KahootNotFoundException;
import com.jpd.web.exception.ModuleNotBelongsToChapterException;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.KahootRepository;
import com.jpd.web.repository.ModuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationResources {
	private final CreatorRepository creatorRepository;
	private final CourseRepository courseRepository;
	private final ChapterRepository chapterRepository;
	private final ModuleRepository moduleRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final KahootRepository kahootRepository;

	public KahootListFunction validateKahootOwnership(long kahootId, String creatorId) {
		log.debug("Validating kahoot {} ownership for creator {}", kahootId, creatorId);

		Creator creator = creatorRepository.findById(creatorId)
				.orElseThrow(() -> new CreatorNotFoundException(creatorId));

		KahootListFunction kahoot = kahootRepository.findById(kahootId)
				.orElseThrow(() -> new KahootNotFoundException(kahootId));

		if (kahoot.getCreator() == null || !creatorId.equals(kahoot.getCreator().getCreatorId())) {
			log.warn("Creator {} attempted to access kahoot {} not owned by them", creatorId, kahootId);
			throw new UnauthorizedException("You don't have permission to modify this kahoot");
		}

		log.debug("Kahoot {} ownership validated for creator {}", kahootId, creatorId);
		return kahoot;
	}

	public Course validateCourseOwnership(long courseId, String creatorId) {
		log.debug("Validating course {} ownership for creator {}", courseId, creatorId);

		Creator creator = creatorRepository.findById(creatorId)
				.orElseThrow(() -> new CreatorNotFoundException(creatorId));

		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException(courseId));

		if (course.getCreator() == null || !creatorId.equals(course.getCreator().getCreatorId())) {
			log.warn("Creator {} attempted to access course {} not owned by them", creatorId, courseId);
			throw new UnauthorizedException("You don't have permission to modify this course");
		}

		log.debug("Course {} ownership validated successfully for creator {}", courseId, creatorId);
		return course;
	}

	public Chapter validateChapterBelongsToCourse(long chapterId, long courseId) {
		log.debug("Validating chapter {} belongs to course {}", chapterId, courseId);

		Chapter chapter = chapterRepository.findById(chapterId)
				.orElseThrow(() -> new ChapterNotFoundException(chapterId));

		Long chapterCourseId = chapter.getCourse() == null ? null : chapter.getCourse().getCourseId();
		if (chapterCourseId == null || !Long.valueOf(courseId).equals(chapterCourseId)) {
			log.warn("Chapter {} does not belong to course {}", chapterId, courseId);
			throw new ChapterNotBelongsToCourseException(chapterId, courseId);
		}

		log.debug("Chapter {} validated successfully for course {}", chapterId, courseId);
		return chapter;
	}

	public Course validateCourseExists(long courseId) {
		return courseRepository.findById(courseId)
				.orElseThrow(() -> new CourseNotFoundException(courseId));
	}

	public Chapter validateChapterExists(long chapterId) {
		return chapterRepository.findById(chapterId)
				.orElseThrow(() -> new ChapterNotFoundException(chapterId));
	}

	public com.jpd.web.model.Module validateModuleBelongsToChapter(long moduleId, long chapterId) {
		log.debug("Validating module {} belongs to chapter {}", moduleId, chapterId);

		com.jpd.web.model.Module module = moduleRepository.findById(moduleId)
				.orElseThrow(() -> new ModuleNotFoundException(moduleId));

		Long moduleChapterId = module.getChapter() == null ? null : module.getChapter().getChapterId();
		if (moduleChapterId == null || !Long.valueOf(chapterId).equals(moduleChapterId)) {
			log.warn("Module {} does not belong to chapter {}", moduleId, chapterId);
			throw new ModuleNotBelongsToChapterException(moduleId, chapterId);
		}

		log.debug("Module {} validated successfully for chapter {}", moduleId, chapterId);
		return module;
	}

	public com.jpd.web.model.Module validateCompleteOwnership(long moduleId, long chapterId, long courseId, String creatorId) {
		log.debug("Validating complete ownership chain for module {}", moduleId);

		Course course = validateCourseOwnership(courseId, creatorId);
		validateChapterBelongsToCourse(chapterId, courseId);
		com.jpd.web.model.Module module = validateModuleBelongsToChapter(moduleId, chapterId);

		log.debug("Complete ownership chain validated successfully for module {}", moduleId);
		return module;
	}

	public Course validateCustomerWithCourse(String customerId, long courseId) {
		Course course = validateCourseExists(courseId);

		if (course.getCreator() != null && course.getCreator().getCreatorId().equals(customerId)) {
			return course;
		}

		Optional<Enrollment> enr = enrollmentRepository.findByCourse_CourseIdAndCustomerId(courseId, customerId);
		if (enr.isEmpty()) {
			throw new UnauthorizedException("You must enroll before learning");
		}

		return course;
	}

	public Enrollment validateCustomerWithCourseGetE(String customerId, long courseId) {
		Course course = validateCourseExists(courseId);

		// If requestor is course creator, no enrollment record is needed -> return null
		if (course.getCreator() != null && course.getCreator().getCreatorId().equals(customerId)) {
			return null;
		}

		Optional<Enrollment> enr = enrollmentRepository.findByCourse_CourseIdAndCustomerId(courseId, customerId);
		if (enr.isEmpty()) {
			throw new UnauthorizedException("You must enroll before learning");
		}
		return enr.get();
	}

	public com.jpd.web.model.Module validateModuleContentOwnerShip(long moduleId, long chapterId, long courseId, String customerId) {
		log.debug("Validating module content ownership for module {}", moduleId);

		Course course = validateCourseExists(courseId);

		Creator creator = creatorRepository.findById(customerId).orElse(null);

		// If not creator of course, validate enrollment
		if (creator == null || course.getCreator() == null || !course.getCreator().getCreatorId().equals(creator.getCreatorId())) {
			validateCustomerWithCourse(customerId, courseId);
		}

		validateChapterBelongsToCourse(chapterId, courseId);
		com.jpd.web.model.Module module = validateModuleBelongsToChapter(moduleId, chapterId);

		log.debug("Module content ownership validated successfully for module {}", moduleId);
		return module;
	}
}
