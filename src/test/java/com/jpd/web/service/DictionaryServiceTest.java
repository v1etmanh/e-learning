package com.jpd.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.dto.DictionaryImportResultDto;
import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Language;
import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private RememberWordRepository repository;

    private final ObjectMapper realMapper = new ObjectMapper();

    @InjectMocks
    private DictionaryService dictionaryService;

    // ObjectMapper is @Autowired-field-injected; @InjectMocks won't populate it since
    // it's not a @Mock, so wire the real one in manually for these JSON-heavy tests.
    private void useRealObjectMapper() {
        org.springframework.test.util.ReflectionTestUtils.setField(dictionaryService, "objectMapper", realMapper);
    }

    @Test
    void getDictionary_mapsRepositoryResultsToDto() {
        RememberWord word = RememberWord.builder().id(1L).word("hello").meaning("xin chao")
                .example(List.of()).synonyms(List.of()).vote(List.of()).language(Language.ENGLISH).build();
        when(repository.findAllByCustomerId("customer-1")).thenReturn(List.of(word));

        List<RememberWordDto> result = dictionaryService.getDictionary("customer-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWord()).isEqualTo("hello");
    }

    @Test
    void addRememberWord_setsCustomerIdAndSaves() {
        RememberWordDto dto = RememberWordDto.builder().word("hello").meaning("xin chao")
                .example(List.of()).synonyms(List.of()).build();

        dictionaryService.addRememberWord("customer-1", dto);

        ArgumentCaptor<RememberWord> captor = ArgumentCaptor.forClass(RememberWord.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo("customer-1");
    }

    @Nested
    class ImportJson {

        @Test
        void throwsIllegalArgument_whenFileNullOrEmpty() {
            assertThrows(IllegalArgumentException.class, () -> dictionaryService.importJson("customer-1", null));
            MockMultipartFile empty = new MockMultipartFile("file", "empty.json", "application/json", new byte[0]);
            assertThrows(IllegalArgumentException.class, () -> dictionaryService.importJson("customer-1", empty));
        }

        @Test
        void throwsIllegalArgument_whenFileExceeds5MB() {
            byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
            MockMultipartFile file = new MockMultipartFile("file", "big.json", "application/json", tooBig);

            assertThrows(IllegalArgumentException.class, () -> dictionaryService.importJson("customer-1", file));
        }

        @Test
        void throwsIllegalArgument_whenJsonMalformed() {
            useRealObjectMapper();
            MockMultipartFile file = new MockMultipartFile("file", "bad.json", "application/json",
                    "{not valid json".getBytes(StandardCharsets.UTF_8));

            assertThrows(IllegalArgumentException.class, () -> dictionaryService.importJson("customer-1", file));
        }

        @Test
        void parsesTopLevelArrayShape() {
            useRealObjectMapper();
            when(repository.findAllByCustomerId("customer-1")).thenReturn(List.of());
            String json = "[{\"word\":\"apple\",\"meaning\":\"qua tao\"}]";
            MockMultipartFile file = new MockMultipartFile("file", "words.json", "application/json",
                    json.getBytes(StandardCharsets.UTF_8));

            DictionaryImportResultDto result = dictionaryService.importJson("customer-1", file);

            assertThat(result.getImportedCount()).isEqualTo(1);
        }

        @Test
        void parsesEntriesWrapperShape() {
            useRealObjectMapper();
            when(repository.findAllByCustomerId("customer-1")).thenReturn(List.of());
            String json = "{\"entries\":[{\"word\":\"apple\",\"meaning\":\"qua tao\"}]}";
            MockMultipartFile file = new MockMultipartFile("file", "words.json", "application/json",
                    json.getBytes(StandardCharsets.UTF_8));

            DictionaryImportResultDto result = dictionaryService.importJson("customer-1", file);

            assertThat(result.getImportedCount()).isEqualTo(1);
        }

        @Test
        void blankWordEntry_countedAsInvalid_notSaved() {
            useRealObjectMapper();
            when(repository.findAllByCustomerId("customer-1")).thenReturn(List.of());
            String json = "[{\"word\":\"  \",\"meaning\":\"x\"}]";
            MockMultipartFile file = new MockMultipartFile("file", "words.json", "application/json",
                    json.getBytes(StandardCharsets.UTF_8));

            DictionaryImportResultDto result = dictionaryService.importJson("customer-1", file);

            assertThat(result.getImportedCount()).isZero();
            assertThat(result.getInvalidCount()).isEqualTo(1);
        }

        @Test
        void skipsWord_alreadyInExistingDictionary() {
            useRealObjectMapper();
            RememberWord existing = RememberWord.builder().word("Apple").build();
            when(repository.findAllByCustomerId("customer-1")).thenReturn(List.of(existing));
            String json = "[{\"word\":\"apple\",\"meaning\":\"qua tao\"}]";
            MockMultipartFile file = new MockMultipartFile("file", "words.json", "application/json",
                    json.getBytes(StandardCharsets.UTF_8));

            DictionaryImportResultDto result = dictionaryService.importJson("customer-1", file);

            assertThat(result.getImportedCount()).isZero();
            assertThat(result.getSkippedCount()).isEqualTo(1);
        }

        @Test
        void skipsDuplicateWord_withinSameFile_keepsOnlyFirstOccurrence() {
            useRealObjectMapper();
            when(repository.findAllByCustomerId("customer-1")).thenReturn(List.of());
            String json = "[{\"word\":\"apple\",\"meaning\":\"first\"},{\"word\":\"Apple\",\"meaning\":\"second\"}]";
            MockMultipartFile file = new MockMultipartFile("file", "words.json", "application/json",
                    json.getBytes(StandardCharsets.UTF_8));

            DictionaryImportResultDto result = dictionaryService.importJson("customer-1", file);

            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
        }

        @Test
        void parseLanguage_recognizesShortCodesAndFallsBackToEnglishForUnknown() {
            useRealObjectMapper();
            when(repository.findAllByCustomerId("customer-1")).thenReturn(List.of());
            String json = "[{\"word\":\"a\",\"language\":\"VI\"},{\"word\":\"b\",\"language\":\"totally-unknown\"}]";
            MockMultipartFile file = new MockMultipartFile("file", "words.json", "application/json",
                    json.getBytes(StandardCharsets.UTF_8));
            ArgumentCaptor<List<RememberWord>> captor = ArgumentCaptor.forClass(List.class);

            dictionaryService.importJson("customer-1", file);

            org.mockito.Mockito.verify(repository).saveAll(captor.capture());
            List<RememberWord> saved = captor.getValue();
            assertThat(saved.get(0).getLanguage()).isEqualTo(Language.VIETNAMESE);
            assertThat(saved.get(1).getLanguage()).isEqualTo(Language.ENGLISH);
        }
    }

    @Nested
    class UpdateDeleteValidate {

        @Test
        void update_throwsRuntimeException_whenIdMissing() {
            when(repository.findById(1L)).thenReturn(Optional.empty());
            RememberWordDto dto = RememberWordDto.builder().rwId(1L).build();

            assertThrows(RuntimeException.class, () -> dictionaryService.updateRememberWord("customer-1", dto));
        }

        @Test
        void update_throwsUnauthorized_whenCustomerMismatch() {
            RememberWord existing = RememberWord.builder().id(1L).customerId("owner-1").build();
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            RememberWordDto dto = RememberWordDto.builder().rwId(1L).build();

            assertThrows(UnauthorizedException.class,
                    () -> dictionaryService.updateRememberWord("someone-else", dto));
        }

        @Test
        void delete_throwsRuntimeException_whenIdMissing() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> dictionaryService.deleteRememberWord("customer-1", 1L));
        }

        @Test
        void delete_succeeds_whenOwnerMatches() {
            RememberWord existing = RememberWord.builder().id(1L).customerId("customer-1").build();
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            dictionaryService.deleteRememberWord("customer-1", 1L);

            org.mockito.Mockito.verify(repository).deleteById(1L);
        }
    }
}
