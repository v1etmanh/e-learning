package com.jpd.web.service;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.dto.PopularCourseDTO;
import com.jpd.web.exception.ApiException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.CourseMetrics;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Language;
import com.jpd.web.model.Module;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CourseMetricsRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.service.utils.CodeGenerator;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private FireBaseService fireBaseService;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ModuleContentRepository moduleContentRepository;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private ValidationResources resourceValidator;
    @Mock
    private CodeGenerator codeGenerator;
    @Mock
    private CourseMetricsRepository courseMetricsRepository;

    @InjectMocks
    private CourseService courseService;

    private CourseFormDto aFormDto() {
        CourseFormDto dto = new CourseFormDto();
        dto.setName("New Course");
        dto.setDescription("desc");
        dto.setTargetAudience("everyone");
        dto.setLanguage(Language.ENGLISH);
        dto.setTeachingLanguage(Language.ENGLISH);
        dto.setAccessMode(AccessMode.PUBLIC);
        dto.setImgFile(new MockMultipartFile("img", "img.png", "image/png", new byte[]{1, 2, 3}));
        return dto;
    }

    @Nested
    class CreateCourse {

        @Test
        void generatesJoinKey_forEveryCourse_regardlessOfAccessMode() throws IOException {
            Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
            when(fireBaseService.uploadFile(any(), eq(com.jpd.web.model.TypeOfFile.IMG)))
                    .thenReturn("https://firebase/img.png");
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));
            when(codeGenerator.generate6DigitCode()).thenReturn("123456");
            when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

            Course result = courseService.createCourse(aFormDto(), "creator-1");

            assertThat(result.getJoinKey()).isEqualTo("123456");
            assertThat(result.getUrlImg()).isEqualTo("https://firebase/img.png");
            assertThat(result.getCreator()).isSameAs(creator);
        }

        @Test
        void savesZeroedCourseMetrics_alongsideCourse() throws IOException {
            Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
            when(fireBaseService.uploadFile(any(), eq(com.jpd.web.model.TypeOfFile.IMG)))
                    .thenReturn("https://firebase/img.png");
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));
            when(codeGenerator.generate6DigitCode()).thenReturn("123456");
            when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

            courseService.createCourse(aFormDto(), "creator-1");

            ArgumentCaptor<CourseMetrics> captor = ArgumentCaptor.forClass(CourseMetrics.class);
            verify(courseMetricsRepository).save(captor.capture());
            assertThat(captor.getValue().getTotalStudents()).isZero();
            assertThat(captor.getValue().getAverageRating()).isZero();
        }

        @Test
        void throwsApiException_whenUploadedUrlBlank() throws IOException {
            when(fireBaseService.uploadFile(any(), eq(com.jpd.web.model.TypeOfFile.IMG))).thenReturn("");

            assertThrows(ApiException.class, () -> courseService.createCourse(aFormDto(), "creator-1"));
        }

        @Test
        void throwsApiException_whenUploadThrowsIOException() throws IOException {
            when(fireBaseService.uploadFile(any(), eq(com.jpd.web.model.TypeOfFile.IMG)))
                    .thenThrow(new IOException("network error"));

            assertThrows(ApiException.class, () -> courseService.createCourse(aFormDto(), "creator-1"));
        }
    }

    @Nested
    class RetrieveCourseByEmail {

        @Test
        void propagatesNoSuchElement_whenCreatorMissing_noOrElseThrowGuard() {
            when(creatorRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> courseService.retrieveCourseByemail("missing"));
        }

        @Test
        void mapsCoursesToCards_whenCreatorExists() {
            Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
            Course course = CourseTestDataBuilder.aCourse().creator(creator).build();
            creator.setCourses(List.of(course));
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            List<CourseCardDto> result = courseService.retrieveCourseByemail("creator-1");

            assertThat(result).hasSize(1);
        }
    }

    @Test
    void getCourseById_delegatesOwnershipCheck_andSetsContentTypesOnEveryModule() {
        Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
        Module module = Module.builder().moduleId(1L).build();
        Chapter chapter = Chapter.builder().chapterId(1L).course(course).modules(List.of(module)).build();
        course.setChapters(List.of(chapter));
        when(resourceValidator.validateCourseOwnership(1L, "creator-1")).thenReturn(course);
        when(moduleContentRepository.findTypeOfContentByModuleId(1L)).thenReturn(Set.of(TypeOfContent.VIDEO));

        CourseContentDto result = courseService.getCourseById(1L, "creator-1");

        assertThat(result).isNotNull();
        assertThat(module.getContentTypes()).containsExactly(TypeOfContent.VIDEO);
    }

    @Test
    void changeCourseSatus_flipsPublicFlag() {
        Course course = CourseTestDataBuilder.aCourse().isPublic(true).build();
        when(resourceValidator.validateCourseOwnership(1L, "creator-1")).thenReturn(course);

        courseService.changeCourseSatus(1L, "creator-1");

        assertThat(course.isPublic()).isFalse();
        verify(courseRepository).save(course);
    }

    @Nested
    class RetrieveCCourse {

        @Test
        void propagatesNoSuchElement_whenCreatorMissing() {
            when(creatorRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> courseService.retrieveCCourse("missing"));
        }

        @Test
        void mapsOwnedCourses() {
            Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
            Course course = CourseTestDataBuilder.aCourse().creator(creator).build();
            course.setCourseMetrics(CourseTestDataBuilder.aMetricsFor(course).build());
            creator.setCourses(List.of(course));
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            List<PopularCourseDTO> result = courseService.retrieveCCourse("creator-1");

            assertThat(result).hasSize(1);
        }
    }
}
