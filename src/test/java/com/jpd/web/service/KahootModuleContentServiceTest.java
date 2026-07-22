package com.jpd.web.service;

import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.MultipleChoiceQuestion;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.PassageRepository;
import com.jpd.web.repository.ReadingQuestionRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.KahootTestDataBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KahootModuleContentServiceTest {

    @Mock
    private ModuleContentRepository moduleContentRepository;
    @Mock
    private ValidationResources validationResources;
    @Mock
    private ReadingQuestionRepository readingQuestionRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private EntityManager entityManager;

    private KahootModuleContentService kahootModuleContentService;

    private ModuleContent aContent(Long mcId) {
        ModuleContent mc = MultipleChoiceQuestion.builder().build();
        mc.setMcId(mcId);
        mc.setTypeOfContent(TypeOfContent.MULTIPLE_CHOICE);
        return mc;
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        kahootModuleContentService = new KahootModuleContentService();
        ReflectionTestUtils.setField(kahootModuleContentService, "moduleContentRepository", moduleContentRepository);
        ReflectionTestUtils.setField(kahootModuleContentService, "validationResources", validationResources);
        ReflectionTestUtils.setField(kahootModuleContentService, "readingQuestionRepository", readingQuestionRepository);
        ReflectionTestUtils.setField(kahootModuleContentService, "passageRepository", passageRepository);
        ReflectionTestUtils.setField(kahootModuleContentService, "entityManager", entityManager);
    }

    @Nested
    class UpdateCourseMaterial {

        @Test
        void delegatesOwnershipCheck_toValidateKahootOwnership() {
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
            when(validationResources.validateKahootOwnership(1L, "creator-1")).thenReturn(kahoot);
            when(moduleContentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<ModuleContent> result = kahootModuleContentService.updateCourseMaterial(new ArrayList<>(), 1L, "creator-1");

            assertThat(result).isEmpty();
        }

        @Test
        void mixedBatch_resetsAllIdsToNull_deletesExistingFirst_thenClearsEntityManager() {
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
            when(validationResources.validateKahootOwnership(1L, "creator-1")).thenReturn(kahoot);
            ModuleContent brandNew = aContent(null);
            ModuleContent existing = aContent(500L);
            when(moduleContentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<ModuleContent> result = kahootModuleContentService.updateCourseMaterial(
                    List.of(brandNew, existing), 1L, "creator-1");

            assertThat(result).allSatisfy(c -> assertThat(c.getMcId()).isNull());
            assertThat(result).allSatisfy(c -> assertThat(c.getKahootListFunction()).isSameAs(kahoot));

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
        void throwsModuleContentNotFound_whenContentMissing() {
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
            when(validationResources.validateKahootOwnership(1L, "creator-1")).thenReturn(kahoot);
            when(moduleContentRepository.findById(10L)).thenReturn(Optional.empty());

            assertThrows(ModuleContentNotFoundException.class,
                    () -> kahootModuleContentService.deleteModuleContent(10L, 1L, "creator-1"));
        }

        @Test
        void throwsUnauthorized_whenContentBelongsToDifferentKahoot() {
            KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
            when(validationResources.validateKahootOwnership(1L, "creator-1")).thenReturn(kahoot);
            KahootListFunction otherKahoot = KahootTestDataBuilder.aKahoot().kahootId(999L).build();
            ModuleContent content = aContent(10L);
            content.setKahootListFunction(otherKahoot);
            when(moduleContentRepository.findById(10L)).thenReturn(Optional.of(content));

            assertThrows(UnauthorizedException.class,
                    () -> kahootModuleContentService.deleteModuleContent(10L, 1L, "creator-1"));
        }

        @Test
        void succeeds_evenForLargeIds_longVsBoxedLongComparisonIsSafeDueToUnboxing() {
            // KahootListFunction.kahootId is a primitive `long`, so the `!=` comparison
            // against the boxed `Long kahootId` parameter auto-unboxes and compares
            // numerically - not a reference-equality trap.
            long largeKahootId = 500_000L;
            KahootListFunction kahoot = KahootTestDataBuilder.aKahoot().kahootId(largeKahootId).build();
            when(validationResources.validateKahootOwnership(largeKahootId, "creator-1")).thenReturn(kahoot);
            ModuleContent content = aContent(10L);
            content.setKahootListFunction(kahoot);
            when(moduleContentRepository.findById(10L)).thenReturn(Optional.of(content));

            kahootModuleContentService.deleteModuleContent(10L, largeKahootId, "creator-1");

            verify(moduleContentRepository).deleteById(10L);
        }
    }

    @Test
    void deleteModuleContentsByType_usesCompleteOwnershipCheck_notKahootOwnership() {
        // Documents an inconsistency: this method validates the full course/chapter/module
        // ownership chain instead of validateKahootOwnership like its sibling methods above.
        com.jpd.web.model.Module module = com.jpd.web.model.Module.builder().moduleId(1L).build();
        when(validationResources.validateCompleteOwnership(1L, 2L, 3L, "creator-1")).thenReturn(module);

        kahootModuleContentService.deleteModuleContentsByType(TypeOfContent.VIDEO, 1L, 2L, 3L, "creator-1");

        verify(validationResources).validateCompleteOwnership(1L, 2L, 3L, "creator-1");
        verify(validationResources, org.mockito.Mockito.never())
                .validateKahootOwnership(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(moduleContentRepository).deleteByTypeOfContentAndModuleId(TypeOfContent.VIDEO, 1L);
    }
}
