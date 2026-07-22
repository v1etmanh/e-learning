package com.jpd.web.service;

import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.MultipleChoiceQuestion;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.repository.PassageRepository;
import com.jpd.web.repository.ReadingQuestionRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleContentServiceTest {

    @Mock
    private ModuleContentRepository moduleContentRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private ValidationResources validationResources;
    @Mock
    private ReadingQuestionRepository readingQuestionRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ModuleContentService moduleContentService;

    private com.jpd.web.model.Module moduleOwnedBy(String creatorId, long moduleId) {
        Course course = CourseTestDataBuilder.aCourseOwnedBy(creatorId);
        Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
        return com.jpd.web.model.Module.builder().moduleId(moduleId).chapter(chapter).build();
    }

    private ModuleContent aContent(Long mcId) {
        ModuleContent mc = MultipleChoiceQuestion.builder().build();
        mc.setMcId(mcId);
        mc.setTypeOfContent(TypeOfContent.MULTIPLE_CHOICE);
        return mc;
    }

    @Nested
    class UpdateCourseMaterial {

        @Test
        void throwsModuleNotFound_whenModuleMissing() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.empty());
            ModuleContentDto dto = new ModuleContentDto(1L, List.of());

            assertThrows(ModuleNotFoundException.class,
                    () -> moduleContentService.updateCourseMaterial(dto, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenCreatorMismatch() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));
            ModuleContentDto dto = new ModuleContentDto(1L, List.of());

            assertThrows(UnauthorizedException.class,
                    () -> moduleContentService.updateCourseMaterial(dto, "someone-else"));
        }

        @Test
        void emptyContentList_returnsEmpty_noRepositoryCalls() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));
            ModuleContentDto dto = new ModuleContentDto(1L, new ArrayList<>());

            List<ModuleContent> result = moduleContentService.updateCourseMaterial(dto, "creator-1");

            assertThat(result).isEmpty();
            verifyNoInteractions(entityManager);
            verify(moduleContentRepository, never()).saveAll(anyList());
        }

        @Test
        void mixedBatch_resetsAllIdsToNull_deletesExistingFirst_thenClearsEntityManager() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));
            ModuleContent brandNew = aContent(null);
            ModuleContent existing = aContent(500L);
            ModuleContentDto dto = new ModuleContentDto(1L, List.of(brandNew, existing));
            when(moduleContentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<ModuleContent> result = moduleContentService.updateCourseMaterial(dto, "creator-1");

            assertThat(result).allSatisfy(c -> assertThat(c.getMcId()).isNull());

            InOrder order = inOrder(moduleContentRepository, entityManager);
            order.verify(moduleContentRepository).deleteAllById(List.of(500L));
            order.verify(moduleContentRepository).flush();
            order.verify(entityManager).clear();
            order.verify(moduleContentRepository).saveAll(anyList());
        }
    }

    @Nested
    class DeleteModuleContent {

        @Test
        void throwsModuleNotFound_fromOwnershipCheck() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ModuleNotFoundException.class,
                    () -> moduleContentService.deleteModuleContent(10L, 1L, "creator-1"));
        }

        @Test
        void throwsModuleContentNotFound_whenContentMissing() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));
            when(moduleContentRepository.findById(10L)).thenReturn(Optional.empty());

            assertThrows(ModuleContentNotFoundException.class,
                    () -> moduleContentService.deleteModuleContent(10L, 1L, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenContentBelongsToDifferentModule() {
            when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));
            ModuleContent content = aContent(10L);
            content.setModuleId(999L);
            when(moduleContentRepository.findById(10L)).thenReturn(Optional.of(content));

            assertThrows(UnauthorizedException.class,
                    () -> moduleContentService.deleteModuleContent(10L, 1L, "creator-1"));
        }

        @Test
        void succeeds_evenForLargeIds_longVsBoxedLongComparisonIsSafeDueToUnboxing() {
            // moduleId on ModuleContent is a primitive `long`, so `!=` against the boxed
            // `Long` parameter auto-unboxes and compares numerically - no reference-equality
            // trap here (unlike ModuleService.updateModuleName's String `!=` comparison).
            long largeModuleId = 500_000L; // well outside the Long boxing cache [-128,127]
            when(moduleRepository.findById(largeModuleId)).thenReturn(Optional.of(moduleOwnedBy("creator-1", largeModuleId)));
            ModuleContent content = aContent(10L);
            content.setModuleId(largeModuleId);
            when(moduleContentRepository.findById(10L)).thenReturn(Optional.of(content));

            moduleContentService.deleteModuleContent(10L, largeModuleId, "creator-1");

            verify(moduleContentRepository).deleteById(10L);
        }
    }

    @Test
    void deleteModuleContentsByType_delegatesAfterOwnershipCheck() {
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));

        moduleContentService.deleteModuleContentsByType(TypeOfContent.VIDEO, 1L, "creator-1");

        verify(moduleContentRepository).deleteByTypeOfContentAndModuleId(TypeOfContent.VIDEO, 1L);
    }

    @Test
    void getModuleContentsByType_delegatesAfterOwnershipCheck() {
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));
        when(moduleContentRepository.findByTypeOfContentAndModuleId(TypeOfContent.VIDEO, 1L))
                .thenReturn(List.of());

        List<ModuleContent> result = moduleContentService.getModuleContentsByType(TypeOfContent.VIDEO, 1L, "creator-1");

        assertThat(result).isEmpty();
    }

    @Test
    void getAllModuleContents_delegatesAfterOwnershipCheck() {
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(moduleOwnedBy("creator-1", 1L)));
        when(moduleContentRepository.findByModuleId(1L)).thenReturn(List.of());

        List<ModuleContent> result = moduleContentService.getAllModuleContents(1L, "creator-1");

        assertThat(result).isEmpty();
    }
}
