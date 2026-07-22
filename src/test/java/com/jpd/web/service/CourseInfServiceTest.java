package com.jpd.web.service;

import com.jpd.web.dto.CourseDescriptionDto;
import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.CourseMetrics;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.model.Language;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.CreatorTestDataBuilder;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseInfServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ValidationResources validationResources;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private CustomerModuleContentRepository contentRepository;

    @InjectMocks
    private CourseInfService courseInfService;

    private Course publicCourseWithScore(String name, double avgRating, int totalStudents) {
        Course course = CourseTestDataBuilder.aCourse().name(name).isPublic(true).build();
        CourseMetrics metrics = CourseTestDataBuilder.aMetricsFor(course)
                .averageRating(avgRating).totalStudents(totalStudents).build();
        course.setCourseMetrics(metrics);
        return course;
    }

    @Nested
    class GetRecommendCourses {

        @Test
        void filtersOutNonPublicCourses() {
            Course privateCourse = publicCourseWithScore("private", 5.0, 100);
            privateCourse.setPublic(false);
            when(courseRepository.findDistinctLanguages()).thenReturn(List.of(Language.ENGLISH));
            when(courseRepository.findByLanguage(Language.ENGLISH)).thenReturn(List.of(privateCourse));

            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            assertThat(result).isEmpty();
        }

        @Test
        void capsAtThree_whenFewerThanThreePublicCourses() {
            Course c1 = publicCourseWithScore("A", 4.0, 10);
            when(courseRepository.findDistinctLanguages()).thenReturn(List.of(Language.ENGLISH));
            when(courseRepository.findByLanguage(Language.ENGLISH)).thenReturn(List.of(c1));

            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            assertThat(result).hasSize(1);
        }

        @Test
        void currentSortIsAscending_soTop3AreActuallyTheLowestScoring() {
            // score = avgRating*0.7 + totalStudents*0.3
            Course best = publicCourseWithScore("best", 5.0, 1000);   // score = 303.5
            Course mid = publicCourseWithScore("mid", 3.0, 50);       // score = 17.1
            Course low = publicCourseWithScore("low", 1.0, 10);       // score = 3.7
            Course lowest = publicCourseWithScore("lowest", 0.5, 1); // score = 0.65
            when(courseRepository.findDistinctLanguages()).thenReturn(List.of(Language.ENGLISH));
            when(courseRepository.findByLanguage(Language.ENGLISH))
                    .thenReturn(List.of(best, mid, low, lowest));

            List<CourseInfDto> result = courseInfService.getRecommendCourses();

            // Documents current (likely unintended) behavior: ascending sort + take-first-3
            // returns the 3 LOWEST-scoring courses, not the top 3.
            assertThat(result).extracting(CourseInfDto::getName)
                    .containsExactly("lowest", "low", "mid");
        }
    }

    @Test
    void searchByKey_trimsKeyBeforeDelegating() {
        Course course = CourseTestDataBuilder.aCourse().build();
        course.setCourseMetrics(CourseTestDataBuilder.aMetricsFor(course).build());
        Pageable pageable = PageRequest.of(0, 10);
        when(courseRepository.searchByKey(eq("java"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(course), pageable, 1));

        Page<CourseInfDto> result = courseInfService.searchByKey("  java  ", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(courseRepository).searchByKey(eq("java"), any(Pageable.class));
    }

    @Nested
    class GetCourseDescription {

        @Test
        void throwsRuntimeException_whenMissing_notABusinessException() {
            when(courseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> courseInfService.getCourseDescription(1L));
        }

        @Test
        void throwsUnauthorized_whenCourseNotPublic() {
            Course course = CourseTestDataBuilder.aCourse().isPublic(false).build();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

            assertThrows(UnauthorizedException.class, () -> courseInfService.getCourseDescription(1L));
        }
    }

    @Nested
    class MapToCourseDescriptionDto {

        @Test
        void roundsAverageRatingToOneDecimal() {
            Course course = CourseTestDataBuilder.aCourse().build();
            CourseMetrics metrics = CourseTestDataBuilder.aMetricsFor(course).averageRating(4.2649).build();
            course.setCourseMetrics(metrics);
            when(enrollmentRepository.findByCourse(course)).thenReturn(List.of());

            CourseDescriptionDto dto = courseInfService.mapToCourseDescriptionDto(course);

            assertThat(dto.getAverageRating()).isEqualTo(4.3);
        }

        @Test
        void sumsTotalModulesAcrossChapters_toleratingNullModulesList() {
            Course course = CourseTestDataBuilder.aCourse().build();
            course.setCourseMetrics(CourseTestDataBuilder.aMetricsFor(course).build());
            Chapter chapterWithModules = CourseTestDataBuilder.aChapterOf(course)
                    .modules(List.of(com.jpd.web.model.Module.builder().moduleId(1L).build(),
                            com.jpd.web.model.Module.builder().moduleId(2L).build()))
                    .build();
            Chapter chapterWithNullModules = CourseTestDataBuilder.aChapterOf(course).modules(null).build();
            course.setChapters(List.of(chapterWithModules, chapterWithNullModules));
            when(enrollmentRepository.findByCourse(course)).thenReturn(List.of());

            CourseDescriptionDto dto = courseInfService.mapToCourseDescriptionDto(course);

            assertThat(dto.getTotalModules()).isEqualTo(2);
        }
    }

    @Nested
    class MapCreatorToDto {

        @Test
        void returnsNull_whenCreatorNull_noRepositoryCalls() {
            Course course = CourseTestDataBuilder.aCourse().creator(null).build();
            course.setCourseMetrics(CourseTestDataBuilder.aMetricsFor(course).build());
            when(enrollmentRepository.findByCourse(course)).thenReturn(List.of());

            CourseDescriptionDto dto = courseInfService.mapToCourseDescriptionDto(course);

            assertThat(dto.getCreator()).isNull();
            verify(creatorRepository, never()).countTotalStudentsByCreatorId(any());
        }
    }

    @Test
    void retrieveYourCourse_usesCustomerModuleContentSizeAsFinishCount() {
        Course course = CourseTestDataBuilder.aCourse().build();
        Chapter chapter = CourseTestDataBuilder.aChapterOf(course).modules(new ArrayList<>()).build();
        course.setChapters(List.of(chapter));

        Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollmentOf("student-1", course);
        enrollment.setCustomerModuleContents(List.of(
                com.jpd.web.model.CustomerModuleContent.builder().build(),
                com.jpd.web.model.CustomerModuleContent.builder().build()));
        when(enrollmentRepository.findByCustomerId("student-1")).thenReturn(List.of(enrollment));

        List<CourseLearningCardDto> result = courseInfService.retrieveYourCourse("student-1");

        assertThat(result).hasSize(1);
    }
}
