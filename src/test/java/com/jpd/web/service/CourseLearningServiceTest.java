package com.jpd.web.service;

import com.jpd.web.controller.creator.CourseController;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.EnrollmentTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseLearningServiceTest {

    @Mock
    private CourseController courseController;
    @Mock
    private ValidationResources validationResources;
    @Mock
    private ModuleContentRepository moduleContentRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private CustomerModuleContentRepository contentRepository;

    private CourseLearningService courseLearningService;

    @BeforeEach
    void setUp() {
        courseLearningService = new CourseLearningService(courseController);
        ReflectionTestUtils.setField(courseLearningService, "validationResources", validationResources);
        ReflectionTestUtils.setField(courseLearningService, "moduleContentRepository", moduleContentRepository);
        ReflectionTestUtils.setField(courseLearningService, "moduleRepository", moduleRepository);
        ReflectionTestUtils.setField(courseLearningService, "contentRepository", contentRepository);
    }

    @Nested
    class GetCourseById {

        @Test
        void creatorOwnBranch_loadsContentTypesForEveryModule() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            Module module = Module.builder().moduleId(1L).build();
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).modules(List.of(module)).build();
            course.setChapters(List.of(chapter));
            when(validationResources.validateCourseExists(1L)).thenReturn(course);
            when(moduleContentRepository.findTypeOfContentByModuleId(1L)).thenReturn(Set.of(TypeOfContent.VIDEO));

            CourseContentDto result = courseLearningService.getCourseById(1L, "creator-1");

            assertThat(result).isNotNull();
            assertThat(module.getContentTypes()).containsExactly(TypeOfContent.VIDEO);
        }

        @Test
        void studentBranch_reDerivesCourseFromEnrollment_throwsUnauthorized_whenNotPublic() {
            Course courseArg = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(validationResources.validateCourseExists(1L)).thenReturn(courseArg);

            Course enrolledCourse = CourseTestDataBuilder.aCourse().isPublic(false).build();
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollmentOf("student-1", enrolledCourse);
            when(validationResources.validateCustomerWithCourseGetE("student-1", 1L)).thenReturn(enrollment);

            assertThrows(UnauthorizedException.class,
                    () -> courseLearningService.getCourseById(1L, "student-1"));
        }

        @Test
        void studentBranch_returnsContent_whenEnrolledCourseIsPublic() {
            Course courseArg = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(validationResources.validateCourseExists(1L)).thenReturn(courseArg);

            Course enrolledCourse = CourseTestDataBuilder.aCourse().isPublic(true).build();
            Module module = Module.builder().moduleId(2L).build();
            Chapter chapter = Chapter.builder().chapterId(2L).course(enrolledCourse).modules(List.of(module)).build();
            enrolledCourse.setChapters(List.of(chapter));
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollmentOf("student-1", enrolledCourse);
            when(validationResources.validateCustomerWithCourseGetE("student-1", 1L)).thenReturn(enrollment);
            when(moduleContentRepository.findTypeOfContentByModuleId(2L)).thenReturn(Set.of());
            when(contentRepository.findByEnrollmentAndModule(enrollment, module)).thenReturn(Optional.empty());

            CourseContentDto result = courseLearningService.getCourseById(1L, "student-1");

            assertThat(result).isNotNull();
        }
    }

    @Test
    void getModuleContentsByTypeAndModuleId_returnsResultsFromRedundantRefetch() {
        Module module = Module.builder().moduleId(1L).build();
        when(validationResources.validateModuleContentOwnerShip(1L, 2L, 3L, "user-1")).thenReturn(module);
        ModuleContent md = com.jpd.web.model.MultipleChoiceQuestion.builder().build();
        md.setMcId(10L);
        when(moduleContentRepository.findByTypeOfContentAndModuleId(TypeOfContent.MULTIPLE_CHOICE, 1L))
                .thenReturn(List.of(md));
        when(moduleContentRepository.findById(10L)).thenReturn(Optional.of(md));

        List<ModuleContent> result = courseLearningService.getModuleContentsByTypeAndModuleId(
                TypeOfContent.MULTIPLE_CHOICE, 1L, 2L, 3L, "user-1");

        assertThat(result).containsExactly(md);
    }

    @Nested
    class UpdateCustomerFinishModule {

        @Test
        void throwsModuleNotFound_whenModuleMissing() {
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
            when(validationResources.validateCustomerWithCourseGetE("user-1", 1L)).thenReturn(enrollment);
            when(moduleRepository.findById(5L)).thenReturn(Optional.empty());

            assertThrows(ModuleNotFoundException.class,
                    () -> courseLearningService.updateCustomerFinishModule(1L, "user-1", 5L, TypeOfContent.VIDEO));
        }

        @Test
        void throwsUnauthorized_whenModuleBelongsToDifferentCourse() {
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
            when(validationResources.validateCustomerWithCourseGetE("user-1", 1L)).thenReturn(enrollment);
            Course otherCourse = CourseTestDataBuilder.aCourse().courseId(99L).build();
            Chapter chapter = Chapter.builder().chapterId(1L).course(otherCourse).build();
            Module module = Module.builder().moduleId(5L).chapter(chapter).build();
            when(moduleRepository.findById(5L)).thenReturn(Optional.of(module));

            assertThrows(UnauthorizedException.class,
                    () -> courseLearningService.updateCustomerFinishModule(1L, "user-1", 5L, TypeOfContent.VIDEO));
        }

        @Test
        void throwsRuntimeException_whenModuleDoesNotContainRequestedContentType() {
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
            when(validationResources.validateCustomerWithCourseGetE("user-1", 1L)).thenReturn(enrollment);
            Course course = CourseTestDataBuilder.aCourse().courseId(1L).build();
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            Module module = Module.builder().moduleId(5L).chapter(chapter).build();
            when(moduleRepository.findById(5L)).thenReturn(Optional.of(module));
            when(moduleContentRepository.findTypeOfContentByModuleId(5L)).thenReturn(Set.of(TypeOfContent.VIDEO));

            assertThrows(RuntimeException.class,
                    () -> courseLearningService.updateCustomerFinishModule(1L, "user-1", 5L, TypeOfContent.WRITING));
        }

        @Test
        void createsNewCustomerModuleContent_withAvailableRequestFive_whenNoneExists() {
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
            when(validationResources.validateCustomerWithCourseGetE("user-1", 1L)).thenReturn(enrollment);
            Course course = CourseTestDataBuilder.aCourse().courseId(1L).build();
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            Module module = Module.builder().moduleId(5L).chapter(chapter).build();
            when(moduleRepository.findById(5L)).thenReturn(Optional.of(module));
            when(moduleContentRepository.findTypeOfContentByModuleId(5L)).thenReturn(Set.of(TypeOfContent.VIDEO));
            when(contentRepository.findByEnrollmentAndModule(enrollment, module)).thenReturn(Optional.empty());

            courseLearningService.updateCustomerFinishModule(1L, "user-1", 5L, TypeOfContent.VIDEO);

            org.mockito.ArgumentCaptor<CustomerModuleContent> captor =
                    org.mockito.ArgumentCaptor.forClass(CustomerModuleContent.class);
            verify(contentRepository).save(captor.capture());
            assertThat(captor.getValue().getAvailableRequest()).isEqualTo(5);
            assertThat(captor.getValue().getTypeOfContent()).containsExactly(TypeOfContent.VIDEO);
        }

        @Test
        void reusesExistingRecord_andAddsTypeToExistingSet() {
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
            when(validationResources.validateCustomerWithCourseGetE("user-1", 1L)).thenReturn(enrollment);
            Course course = CourseTestDataBuilder.aCourse().courseId(1L).build();
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            Module module = Module.builder().moduleId(5L).chapter(chapter).build();
            when(moduleRepository.findById(5L)).thenReturn(Optional.of(module));
            when(moduleContentRepository.findTypeOfContentByModuleId(5L)).thenReturn(Set.of(TypeOfContent.VIDEO));

            Set<TypeOfContent> existingTypes = new HashSet<>(Set.of(TypeOfContent.WRITING));
            CustomerModuleContent existing = CustomerModuleContent.builder().cqId(1L).availableRequest(3).build();
            existing.setTypeOfContent(existingTypes);
            when(contentRepository.findByEnrollmentAndModule(enrollment, module)).thenReturn(Optional.of(existing));

            courseLearningService.updateCustomerFinishModule(1L, "user-1", 5L, TypeOfContent.VIDEO);

            verify(contentRepository).save(existing);
            assertThat(existing.getTypeOfContent()).containsExactlyInAnyOrder(TypeOfContent.WRITING, TypeOfContent.VIDEO);
            assertThat(existing.getAvailableRequest()).isEqualTo(3);
        }
    }
}
