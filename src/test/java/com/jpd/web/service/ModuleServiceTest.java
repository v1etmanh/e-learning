package com.jpd.web.service;

import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CourseTestDataBuilder;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private ModuleContentRepository moduleContentRepository;
    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private ModuleService moduleService;

    @Test
    void createModule_validatesOwnershipChain_thenSaves() {
        Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
        Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
        when(validationResources.validateCourseOwnership(10L, "creator-1")).thenReturn(course);
        when(validationResources.validateChapterBelongsToCourse(1L, 10L)).thenReturn(chapter);
        when(moduleRepository.save(org.mockito.ArgumentMatchers.any(Module.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Module result = moduleService.createModule("New Module", "creator-1", 10L, 1L);

        assertThat(result.getTitleOfModule()).isEqualTo("New Module");
        assertThat(result.getChapter()).isSameAs(chapter);
    }

    @Test
    void deleteModule_deletesContentThenModule_afterCompleteOwnershipCheck() {
        Module module = Module.builder().moduleId(5L).build();
        when(validationResources.validateCompleteOwnership(5L, 1L, 10L, "creator-1")).thenReturn(module);

        moduleService.deleteModule("creator-1", 10L, 1L, 5L);

        verify(moduleContentRepository).deleteByModuleId(5L);
        verify(moduleRepository).deleteByModuleId(5L);
    }

    @Nested
    class UpdateModuleName {

        @Test
        void throwsModuleNotFound_whenModuleMissing() {
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(moduleRepository.findById(5L)).thenReturn(Optional.empty());

            assertThrows(ModuleNotFoundException.class,
                    () -> moduleService.updateModuleName("creator-1", 5L, "New Title"));
        }

        @Test
        void updatesTitle_whenCallerIsLegitimateOwner_sameStringLiteralInstance() {
            // The `!=` comparison in updateModuleName happens to hold here only because
            // both sides are the exact same interned String literal instance.
            Course course = CourseTestDataBuilder.aCourseOwnedBy("creator-1");
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            Module module = Module.builder().moduleId(5L).chapter(chapter).build();
            when(creatorRepository.findById("creator-1"))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));
            when(moduleRepository.findById(5L)).thenReturn(Optional.of(module));

            moduleService.updateModuleName("creator-1", 5L, "New Title");

            assertThat(module.getTitleOfModule()).isEqualTo("New Title");
        }

        @Test
        void bug_referenceInequality_incorrectlyRejectsLegitimateOwner_whenIdIsANonInternedEqualString() {
            // ModuleService.updateModuleName compares creator ids with `!=` instead of
            // `.equals()`. Two Strings that are .equals() but not the same reference
            // (e.g. one built via `new String(...)`, as a JPA-loaded id would be) make
            // this incorrectly throw UnauthorizedException for the actual owner.
            String storedCreatorId = new String("creator-1");
            String requestCreatorId = new String("creator-1");
            Course course = CourseTestDataBuilder.aCourseOwnedBy(storedCreatorId);
            Chapter chapter = Chapter.builder().chapterId(1L).course(course).build();
            Module module = Module.builder().moduleId(5L).chapter(chapter).build();
            when(creatorRepository.findById(requestCreatorId))
                    .thenReturn(Optional.of(CreatorTestDataBuilder.aCreatorWithId(requestCreatorId)));
            when(moduleRepository.findById(5L)).thenReturn(Optional.of(module));

            assertThrows(UnauthorizedException.class,
                    () -> moduleService.updateModuleName(requestCreatorId, 5L, "New Title"));
        }
    }
}
