package com.jpd.web.service;

import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.EnrollmentExistException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.CourseMetricsHelper;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.EnrollmentTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private ValidationResources validationResources;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseMetricsHelper courseMetricsHelper;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Nested
    class FindByCourseId {

        @Test
        void throwsCourseNotFound_whenCallerIsNotCourseCreator() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("owner-1");
            when(validationResources.validateCourseExists(1L)).thenReturn(course);

            assertThrows(CourseNotFoundException.class,
                    () -> enrollmentService.findByCourseId(1L, "someone-else"));
        }

        @Test
        void returnsEnrollments_whenCallerIsCourseCreator() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("owner-1");
            Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollmentOf("student-1", course);
            when(validationResources.validateCourseExists(1L)).thenReturn(course);
            when(enrollmentRepository.findByCourse(course)).thenReturn(List.of(enrollment));

            List<Enrollment> result = enrollmentService.findByCourseId(1L, "owner-1");

            assertThat(result).containsExactly(enrollment);
        }
    }

    @Nested
    class HandleEnrollCourse {

        @Test
        void throwsCourseNotFound_whenCourseMissing() {
            when(courseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CourseNotFoundException.class,
                    () -> enrollmentService.handleEnrollCouse(1L, "code", "student-1"));
        }

        @Test
        void throwsEnrollmentExist_whenAlreadyEnrolled() {
            Course course = CourseTestDataBuilder.aCourse().build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                    .thenReturn(Optional.of(EnrollmentTestDataBuilder.anEnrollmentOf("student-1", course)));

            assertThrows(EnrollmentExistException.class,
                    () -> enrollmentService.handleEnrollCouse(1L, "code", "student-1"));
        }

        @Test
        void returnsFalse_withoutSavingOrIncrementing_whenPrivateCourseWrongJoinKey() {
            Course course = CourseTestDataBuilder.aCourse()
                    .accessMode(AccessMode.PRIVATE)
                    .joinKey("correct-key")
                    .build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                    .thenReturn(Optional.empty());

            boolean result = enrollmentService.handleEnrollCouse(1L, "wrong-key", "student-1");

            assertThat(result).isFalse();
            verify(enrollmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
            verify(courseMetricsHelper, never()).incrementEnrollmentCount(org.mockito.ArgumentMatchers.any());
        }

        @Test
        void succeeds_whenPrivateCourseCorrectJoinKey() {
            Course course = CourseTestDataBuilder.aCourse()
                    .accessMode(AccessMode.PRIVATE)
                    .joinKey("correct-key")
                    .build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                    .thenReturn(Optional.empty());

            boolean result = enrollmentService.handleEnrollCouse(1L, "correct-key", "student-1");

            assertThat(result).isTrue();
            verify(enrollmentRepository).save(org.mockito.ArgumentMatchers.any(Enrollment.class));
            verify(courseMetricsHelper).incrementEnrollmentCount(1L);
        }

        @Test
        void succeeds_whenPublicCourse_ignoresJoinKey() {
            Course course = CourseTestDataBuilder.aCourse()
                    .accessMode(AccessMode.PUBLIC)
                    .joinKey(null)
                    .build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                    .thenReturn(Optional.empty());

            boolean result = enrollmentService.handleEnrollCouse(1L, "irrelevant-or-null", "student-1");

            assertThat(result).isTrue();
            verify(enrollmentRepository).save(org.mockito.ArgumentMatchers.any(Enrollment.class));
            verify(courseMetricsHelper).incrementEnrollmentCount(1L);
        }
    }
}
