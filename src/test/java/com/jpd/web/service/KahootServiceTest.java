package com.jpd.web.service;

import com.jpd.web.dto.KahootDto;
import com.jpd.web.model.Creator;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.KahootRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import com.jpd.web.testutil.KahootTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KahootServiceTest {

    @Mock
    private KahootRepository kahootRepository;
    @Mock
    private ValidationResources validationResources;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private ModuleContentRepository moduleContentRepository;

    @InjectMocks
    private KahootService kahootService;

    @Test
    void retrieveAll_mapsCreatorsKahoots() {
        Creator creator = CreatorTestDataBuilder.aCreator().build();
        KahootListFunction kahoot = KahootTestDataBuilder.aKahoot().build();
        creator.setKahootListFunctions(List.of(kahoot));
        when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

        List<KahootDto> result = kahootService.retrieveAll("creator-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void createKahoot_savesWithCreator() {
        Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
        when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));
        when(kahootRepository.save(org.mockito.ArgumentMatchers.any(KahootListFunction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        KahootListFunction result = kahootService.createKahoot("My Quiz", "creator-1");

        assertThat(result.getTitle()).isEqualTo("My Quiz");
        assertThat(result.getCreator()).isSameAs(creator);
    }

    @Test
    void deleteKahoot_deletesModuleContentThenKahoot_afterOwnershipCheck() {
        KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
        when(validationResources.validateKahootOwnership(1L, "creator-1")).thenReturn(kahoot);

        kahootService.deleteKahoot("creator-1", 1L);

        verify(moduleContentRepository).deleteByKahootId(1L);
        verify(kahootRepository).deleteById(1L);
    }

    @Test
    void updateKahootTitle_updatesAfterOwnershipCheck() {
        KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
        when(validationResources.validateKahootOwnership(1L, "creator-1")).thenReturn(kahoot);

        kahootService.updateKahootTitle("creator-1", 1L, "New Title");

        assertThat(kahoot.getTitle()).isEqualTo("New Title");
        verify(kahootRepository).save(kahoot);
    }

    @Test
    void retrieveData_returnsModuleContent_afterOwnershipCheck() {
        KahootListFunction kahoot = KahootTestDataBuilder.aKahootOwnedBy("creator-1");
        kahoot.setModuleContent(List.of());
        when(validationResources.validateKahootOwnership(1L, "creator-1")).thenReturn(kahoot);

        List<ModuleContent> result = kahootService.retrieveData("creator-1", 1L);

        assertThat(result).isEmpty();
    }
}
