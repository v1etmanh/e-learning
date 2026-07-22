package com.jpd.web.service.utils;

import com.jpd.web.exception.ChapterNotBelongsToCourseException;
import com.jpd.web.exception.ChapterNotFoundException;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.exception.KahootNotFoundException;
import com.jpd.web.exception.ModuleNotBelongsToChapterException;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
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
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import com.jpd.web.testutil.EnrollmentTestDataBuilder;
import com.jpd.web.testutil.KahootTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationResourcesTest {

    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private KahootRepository kahootRepository;

    private ValidationResources validationResources;

    @BeforeEach
    void setUp() {
        validationResources = new ValidationResources(
                creatorRepository, courseRepository, chapterRepository,
                moduleRepository, enrollmentRepository, kahootRepository);
    }

    @Nested
    class KahootOwnership {

        @Test
        void throwsCreatorNotFound_whenCreatorMissing() {
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.empty());

            assertThrows(CreatorNotFoundException.class,
                    () -> validationResources.validateKahootOwnership(1L, "creator-1"));
        }

        @Test
        void throwsKahootNotFound_whenKahootMissing() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(kahootRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(KahootNotFoundException.class,
                    () -> validationResources.validateKahootOwnership(1L, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenKahootHasNoCreator() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            KahootListFunction kahoot = KahootTestDataBuilder.aKahoot().creator(null).build();
            when(kahootRepository.findById(1L)).thenReturn(Optional.of(kahoot));

            assertThrows(UnauthorizedException.class,
                    () -> validationResources.validateKahootOwnership(1L, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenCreatorMismatch() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(kahootRepository.findById(1L))
                    .thenReturn(Optional.of(KahootTestDataBuilder.aKahootOwnedBy("someone-else")));

            assertThrows(UnauthorizedException.class,
                    () -> validationResources.validateKahootOwnership(1L, "creator-1"));
        }

        @Test
        void returnsKahoot_whenOwnedByCaller() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
            when(kahootRepository.findById(1L)).thenReturn(Optional.of(kahoot));

            KahootListFunction result = validationResources.validateKahootOwnership(1L, "creator-1");

            assertThat(result).isSameAs(kahoot);
        }
    }

    @Nested
    class CourseOwnership {

        @Test
        void throwsCreatorNotFound_whenCreatorMissing() {
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.empty());

            assertThrows(CreatorNotFoundException.class,
                    () -> validationResources.validateCourseOwnership(1L, "creator-1"));
        }

        @Test
        void throwsCourseNotFound_whenCourseMissing() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(courseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CourseNotFoundException.class,
                    () -> validationResources.validateCourseOwnership(1L, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenCourseHasNoCreator() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            Course course = CourseTestDataBuilder.aCourse().creator(null).build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            assertThrows(UnauthorizedException.class,
                    () -> validationResources.validateCourseOwnership(1L, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenCreatorMismatch() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(courseRepository.findById(1L))
                    .thenReturn(Optional.of(CourseTestDataBuilder.aCourseOwnedBy("someone-else")));

            assertThrows(UnauthorizedException.class,
                    () -> validationResources.validateCourseOwnership(1L, "creator-1"));
        }

        @Test
        void returnsCourse_whenOwnedByCaller() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            Course result = validationResources.validateCourseOwnership(1L, "creator-1");

            assertThat(result).isSameAs(course);
        }
    }

    @Nested
    class ChapterBelongsToCourse {

        @Test
        void throwsChapterNotFound_whenChapterMissing() {
            when(chapterRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ChapterNotFoundException.class,
                    () -> validationResources.validateChapterBelongsToCourse(1L, 10L));
        }

        @Test
        void throwsChapterNotBelongsToCourse_whenChapterHasNoCourse() {
            Chapter chapter = Chapter.builder().chapterId(1L).course(null).build();
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

            assertThrows(ChapterNotBelongsToCourseException.class,
                    () -> validationResources.validateChapterBelongsToCourse(1L, 10L));
        }

        @Test
        void throwsChapterNotBelongsToCourse_whenCourseIdDiffers() {
            Course course = CourseTestDataBuilder.aCourse().courseId(99L).build();
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

            assertThrows(ChapterNotBelongsToCourseException.class,
                    () -> validationResources.validateChapterBelongsToCourse(1L, 10L));
        }

        @Test
        void returnsChapter_whenBelongsToCourse() {
            Course course = CourseTestDataBuilder.aCourse().courseId(10L).build();
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

            Chapter result = validationResources.validateChapterBelongsToCourse(1L, 10L);

            assertThat(result).isSameAs(chapter);
        }
    }

    @Nested
    class ModuleBelongsToChapter {

        @Test
        void throwsModuleNotFound_whenModuleMissing() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ModuleNotFoundException.class,
                    () -> validationResources.validateModuleBelongsToChapter(1L, 10L));
        }

        @Test
        void throwsModuleNotBelongsToChapter_whenModuleHasNoChapter() {
            com.jpd.web.model.Module module = com.jpd.web.model.Module.builder().moduleId(1L).chapter(null).build();
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));

            assertThrows(ModuleNotBelongsToChapterException.class,
                    () -> validationResources.validateModuleBelongsToChapter(1L, 10L));
        }

        @Test
        void throwsModuleNotBelongsToChapter_whenChapterIdDiffers() {
            Chapter chapter = Chapter.builder().chapterId(99L).build();
            com.jpd.web.model.Module module = com.jpd.web.model.Module.builder().moduleId(1L).chapter(chapter).build();
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));

            assertThrows(ModuleNotBelongsToChapterException.class,
                    () -> validationResources.validateModuleBelongsToChapter(1L, 10L));
        }

        @Test
        void returnsModule_whenBelongsToChapter() {
            Chapter chapter = Chapter.builder().chapterId(10L).build();
            com.jpd.web.model.Module module = com.jpd.web.model.Module.builder().moduleId(1L).chapter(chapter).build();
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));

            com.jpd.web.model.Module result = validationResources.validateModuleBelongsToChapter(1L, 10L);

            assertThat(result).isSameAs(module);
        }
    }

    @Nested
    class CompleteOwnership {

        @Test
        void validatesCourseThenChapterThenModule_inOrder() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            course.setCourseId(10L);
            Chapter chapter = Chapter.builder().chapterId(20L).course(course).build();
            com.jpd.web.model.Module module = com.jpd.web.model.Module.builder().moduleId(30L).chapter(chapter).build();

            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
            when(chapterRepository.findById(20L)).thenReturn(Optional.of(chapter));
            when(moduleRepository.findById(30L)).thenReturn(Optional.of(module));

            com.jpd.web.model.Module result =
                    validationResources.validateCompleteOwnership(30L, 20L, 10L, "creator-1");

            assertThat(result).isSameAs(module);

            InOrder order = inOrder(courseRepository, chapterRepository, moduleRepository);
            order.verify(courseRepository).findById(10L);
            order.verify(chapterRepository).findById(20L);
            order.verify(moduleRepository).findById(30L);
        }

        @Test
        void shortCircuits_whenCourseOwnershipFails_neverChecksChapterOrModule() {
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.empty());

            assertThrows(CreatorNotFoundException.class,
                    () -> validationResources.validateCompleteOwnership(30L, 20L, 10L, "creator-1"));

            verify(chapterRepository, never()).findById(anyLong());
            verify(moduleRepository, never()).findById(anyLong());
        }
    }

    @Nested
    class CustomerWithCourse {

        @Test
        void returnsCourse_whenCallerIsCreator_noEnrollmentLookup() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            Course result = validationResources.validateCustomerWithCourse("creator-1", 1L);

            assertThat(result).isSameAs(course);
            verify(enrollmentRepository, never()).findByCourse_CourseIdAndCustomerId(anyLong(), anyString());
        }

        @Test
        void returnsCourse_whenCustomerEnrolled() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                    .thenReturn(Optional.of(EnrollmentTestDataBuilder.anEnrollmentOf("student-1", course)));

            Course result = validationResources.validateCustomerWithCourse("student-1", 1L);

            assertThat(result).isSameAs(course);
        }

        @Test
        void throwsUnauthorized_whenNeitherOwnerNorEnrolled() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "stranger"))
                    .thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class,
                    () -> validationResources.validateCustomerWithCourse("stranger", 1L));
        }
    }

    @Nested
    class CustomerWithCourseGetEnrollment {

        @Test
        void returnsNull_whenCallerIsCreator() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            Enrollment result = validationResources.validateCustomerWithCourseGetE("creator-1", 1L);

            assertThat(result).isNull();
            verify(enrollmentRepository, never()).findByCourse_CourseIdAndCustomerId(anyLong(), anyString());
        }

        @Test
        void returnsEnrollment_whenCustomerEnrolled() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollmentOf("student-1", course);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                    .thenReturn(Optional.of(enrollment));

            Enrollment result = validationResources.validateCustomerWithCourseGetE("student-1", 1L);

            assertThat(result).isSameAs(enrollment);
        }

        @Test
        void throwsUnauthorized_whenNeitherOwnerNorEnrolled() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "stranger"))
                    .thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class,
                    () -> validationResources.validateCustomerWithCourseGetE("stranger", 1L));
        }
    }

    @Nested
    class ModuleContentOwnership {

        @Test
        void skipsEnrollmentCheck_whenCallerIsCourseCreator() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            course.setCourseId(10L);
            Chapter chapter = Chapter.builder().chapterId(20L).course(course).build();
            com.jpd.web.model.Module module = com.jpd.web.model.Module.builder().moduleId(30L).chapter(chapter).build();

            when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(course.getCreator()));
            when(chapterRepository.findById(20L)).thenReturn(Optional.of(chapter));
            when(moduleRepository.findById(30L)).thenReturn(Optional.of(module));

            com.jpd.web.model.Module result =
                    validationResources.validateModuleContentOwnerShip(30L, 20L, 10L, "creator-1");

            assertThat(result).isSameAs(module);
            verify(enrollmentRepository, never()).findByCourse_CourseIdAndCustomerId(anyLong(), anyString());
        }

        @Test
        void fallsBackToEnrollmentCheck_whenCallerIsNotCreator() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            course.setCourseId(10L);
            Chapter chapter = Chapter.builder().chapterId(20L).course(course).build();
            com.jpd.web.model.Module module = com.jpd.web.model.Module.builder().moduleId(30L).chapter(chapter).build();
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollmentOf("student-1", course);

            when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
            when(creatorRepository.findById("student-1")).thenReturn(Optional.empty());
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(10L, "student-1"))
                    .thenReturn(Optional.of(enrollment));
            when(chapterRepository.findById(20L)).thenReturn(Optional.of(chapter));
            when(moduleRepository.findById(30L)).thenReturn(Optional.of(module));

            com.jpd.web.model.Module result =
                    validationResources.validateModuleContentOwnerShip(30L, 20L, 10L, "student-1");

            assertThat(result).isSameAs(module);
            verify(enrollmentRepository).findByCourse_CourseIdAndCustomerId(10L, "student-1");
        }
    }
}
