package com.jpd.web.service;

import com.jpd.web.dto.AdminCourseDetailDto;
import com.jpd.web.dto.AdminCourseDto;
import com.jpd.web.dto.CourseDescriptionDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.model.Report;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.EnrollmentTestDataBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceTest {

    @Mock
    private CourseInfService courseInfService;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AdminCourseService adminCourseService;

    @Nested
    class GetAllCourses {

        @Test
        void usesFindAll_whenSearchBlank() {
            Course course = CourseTestDataBuilder.aCourse().reports(new ArrayList<>()).enrollments(new ArrayList<>()).build();
            when(courseRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(course)));

            Page<AdminCourseDto> result = adminCourseService.getAllCourses(0, 10, "");

            assertThat(result.getContent()).hasSize(1);
            verify(courseRepository).findAll(any(Pageable.class));
        }

        @Test
        void usesNameOrDescriptionSearch_whenSearchProvided() {
            when(courseRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    eq("java"), eq("java"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            adminCourseService.getAllCourses(0, 10, "java");

            verify(courseRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    eq("java"), eq("java"), any(Pageable.class));
        }
    }

    @Nested
    class GetCourseById {

        @Test
        void throwsCourseNotFound_whenMissing() {
            when(courseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CourseNotFoundException.class, () -> adminCourseService.getCourseById(1L));
        }

        @Test
        void delegatesToCourseInfService_andWrapsReports() {
            Course course = CourseTestDataBuilder.aCourse()
                    .reports(List.of(Report.builder().reportId(1L).build()))
                    .build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            CourseDescriptionDto descriptionDto = CourseDescriptionDto.builder().build();
            when(courseInfService.mapToCourseDescriptionDto(course)).thenReturn(descriptionDto);

            AdminCourseDetailDto result = adminCourseService.getCourseById(1L);

            assertThat(result).isNotNull();
            verify(courseInfService).mapToCourseDescriptionDto(course);
        }
    }

    @Nested
    class BanUnbanToggle {

        @Test
        void banCourse_throwsNotFound_whenMissing() {
            when(courseRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(CourseNotFoundException.class, () -> adminCourseService.banCourse(1L));
        }

        @Test
        void banCourse_setsBanTrue() {
            Course course = CourseTestDataBuilder.aCourse().isBan(false).build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            adminCourseService.banCourse(1L);

            assertThat(course.isBan()).isTrue();
        }

        @Test
        void unbanCourse_setsBanFalse() {
            Course course = CourseTestDataBuilder.aCourse().isBan(true).build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            adminCourseService.unbanCourse(1L);

            assertThat(course.isBan()).isFalse();
        }

        @Test
        void toggleCoursePublicStatus_flipsPublicFlag() {
            Course course = CourseTestDataBuilder.aCourse().isPublic(true).build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            adminCourseService.toggleCoursePublicStatus(1L);

            assertThat(course.isPublic()).isFalse();
        }
    }

    @Nested
    class RatingCalculation {

        @Test
        void emptyEnrollments_yieldsZeroRatingAndZeroStudents() {
            Course course = CourseTestDataBuilder.aCourse()
                    .enrollments(new ArrayList<>())
                    .reports(new ArrayList<>())
                    .build();
            when(courseRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(course)));

            Page<AdminCourseDto> result = adminCourseService.getAllCourses(0, 10, null);

            AdminCourseDto dto = result.getContent().get(0);
            assertThat(dto.getAvtRating()).isEqualTo(0.0);
            assertThat(dto.getNumberStudent()).isEqualTo(0);
        }

        @Test
        void averageOnlyOverEnrollmentsWithFeedback_butStudentCountIsAllEnrollments() {
            Course course = CourseTestDataBuilder.aCourse().reports(new ArrayList<>()).build();
            Enrollment rated1 = EnrollmentTestDataBuilder.anEnrollmentOf("s1", course);
            rated1.setFeedback(Feedback.builder().rate(4).build());
            Enrollment rated2 = EnrollmentTestDataBuilder.anEnrollmentOf("s2", course);
            rated2.setFeedback(Feedback.builder().rate(5).build());
            Enrollment unrated = EnrollmentTestDataBuilder.anEnrollmentOf("s3", course);
            course.setEnrollments(List.of(rated1, rated2, unrated));

            when(courseRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(course)));

            Page<AdminCourseDto> result = adminCourseService.getAllCourses(0, 10, null);

            AdminCourseDto dto = result.getContent().get(0);
            assertThat(dto.getAvtRating()).isCloseTo(4.5, within(1e-9));
            assertThat(dto.getNumberStudent()).isEqualTo(3);
        }
    }
}
