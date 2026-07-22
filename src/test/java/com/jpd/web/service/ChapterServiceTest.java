package com.jpd.web.service;

import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private ModuleContentRepository moduleContentRepository;
    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private ChapterService chapterService;

    @Test
    void createChapter_validatesCourseOwnership_thenSaves() {
        Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
        when(validationResources.validateCourseOwnership(10L, "creator-1")).thenReturn(course);
        when(chapterRepository.save(org.mockito.ArgumentMatchers.any(Chapter.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Chapter result = chapterService.createChapter("Chapter 1", 10L, "creator-1");

        assertThat(result.getChapterName()).isEqualTo("Chapter 1");
        assertThat(result.getCourse()).isSameAs(course);
    }

    @Test
    void deleteChapter_deletesModuleContentsThenModulesThenChapter() {
        Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
        Module module1 = Module.builder().moduleId(1L).build();
        Module module2 = Module.builder().moduleId(2L).build();
        Chapter chapter = Chapter.builder().chapterId(1L).course(course).modules(List.of(module1, module2)).build();
        when(validationResources.validateCourseOwnership(10L, "creator-1")).thenReturn(course);
        when(validationResources.validateChapterBelongsToCourse(1L, 10L)).thenReturn(chapter);

        chapterService.deleteChapter(1L, 10L, "creator-1");

        verify(moduleContentRepository).deleteByModuleId(1L);
        verify(moduleContentRepository).deleteByModuleId(2L);
        verify(moduleRepository).deleteByChapterChapterId(1L);
        verify(chapterRepository).deleteByChapterId(1L);
    }

    @Nested
    class UpdateChapter {

        @Test
        void throwsModuleNotFound_notChapterNotFound_whenChapterMissing() {
            // Documents an apparent copy-paste bug: the exception raised for a missing
            // *chapter* is ModuleNotFoundException, not ChapterNotFoundException.
            when(chapterRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ModuleNotFoundException.class,
                    () -> chapterService.updateChapter("creator-1", "New Name", 1L));
        }

        @Test
        void throwsUnauthorized_whenCreatorMismatch() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("owner-1");
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

            assertThrows(UnauthorizedException.class,
                    () -> chapterService.updateChapter("someone-else", "New Name", 1L));
        }

        @Test
        void updatesName_whenOwnerMatches() {
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

            chapterService.updateChapter("creator-1", "New Name", 1L);

            assertThat(chapter.getChapterName()).isEqualTo("New Name");
        }
    }
}
