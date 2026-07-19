package com.jpd.web.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.dto.DictionaryImportEntryDto;
import com.jpd.web.dto.DictionaryImportResultDto;
import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Language;
import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.RememberTransform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;

@Service
@Slf4j

public class DictionaryService {
@Autowired
   private RememberWordRepository repository;

@Autowired
private ObjectMapper objectMapper;


   
 
    public List<RememberWordDto> getDictionary(String customerId) {
        List<RememberWord> rememberWords = repository.findAllByCustomerId(customerId);
        return rememberWords.stream().map(e->RememberTransform.toRememberWordDto(e)).toList();
    }
    public RememberWordDto addRememberWord(String customerId,RememberWordDto rememberWordDto) {


                RememberWord rememberWord = RememberTransform.toRememberWord(rememberWordDto);
               
                rememberWord.setCustomerId(customerId);
                repository.save(rememberWord);
                log.info("success to add new remember word {}", rememberWord.getWord());
                return rememberWordDto;

    }

    @Transactional
    public DictionaryImportResultDto importJson(String customerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("JSON file không được để trống");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("JSON file vượt quá giới hạn 5MB");
        }

        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            List<DictionaryImportEntryDto> entries = readEntries(root);
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("JSON không chứa danh sách entries");
            }

            Set<String> existingWords = repository.findAllByCustomerId(customerId).stream()
                    .map(RememberWord::getWord)
                    .filter(word -> word != null)
                    .map(this::normalizeWord)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            List<RememberWord> toSave = new ArrayList<>();
            List<String> skippedWords = new ArrayList<>();
            List<String> invalidReasons = new ArrayList<>();
            Set<String> importedInFile = new LinkedHashSet<>();
            int invalidCount = 0;

            for (DictionaryImportEntryDto entry : entries) {
                String word = entry != null ? normalizeWord(entry.getWord()) : "";
                if (word.isBlank()) {
                    invalidCount++;
                    invalidReasons.add("Một entry không có word");
                    continue;
                }

                if (existingWords.contains(word) || !importedInFile.add(word)) {
                    skippedWords.add(word);
                    continue;
                }

                RememberWord rememberWord = RememberWord.builder()
                        .word(entry.getWord().trim())
                        .meaning(safeText(entry.getMeaning(), "Chưa có định nghĩa"))
                        .description(safeText(entry.getDescription(), "Imported from JPD Vocabulary extension"))
                        .synonyms(cleanList(entry.getSynonyms()))
                        .example(mergeExamples(entry.getExample(), entry.getContext()))
                        .language(parseLanguage(entry.getLanguage()))
                        .customerId(customerId)
                        .build();
                toSave.add(rememberWord);
            }

            repository.saveAll(toSave);
            return DictionaryImportResultDto.builder()
                    .importedCount(toSave.size())
                    .skippedCount(skippedWords.size())
                    .invalidCount(invalidCount)
                    .skippedWords(skippedWords)
                    .invalidReasons(invalidReasons)
                    .build();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc JSON file: " + exception.getMessage(), exception);
        }
    }

    private List<DictionaryImportEntryDto> readEntries(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of();
        }
        if (root.isArray()) {
            return objectMapper.convertValue(root, new TypeReference<List<DictionaryImportEntryDto>>() {});
        }
        JsonNode entriesNode = root.get("entries");
        if (entriesNode == null || !entriesNode.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(entriesNode, new TypeReference<List<DictionaryImportEntryDto>>() {});
    }

    private String normalizeWord(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<String> mergeExamples(List<String> examples, String context) {
        List<String> merged = new ArrayList<>(cleanList(examples));
        if (context != null && !context.isBlank() && !merged.contains(context.trim())) {
            merged.add(context.trim());
        }
        return merged;
    }

    private Language parseLanguage(String value) {
        if (value == null || value.isBlank()) {
            return Language.ENGLISH;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("EN".equals(normalized)) {
            return Language.ENGLISH;
        }
        if ("VI".equals(normalized)) {
            return Language.VIETNAMESE;
        }
        try {
            return Language.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return Language.ENGLISH;
        }
    }
    public RememberWordDto updateRememberWord(String customerId,RememberWordDto rememberWordDto) {
       
       long id=rememberWordDto.getRwId();
       validate(customerId,id);
    	RememberWord rememberWord= repository.findById(rememberWordDto.getRwId()).orElseThrow(()-> new RuntimeException("Remember word not found"));
        //so sanh voi ai nguoi dung
          RememberWord re=RememberTransform.toRememberWord(rememberWordDto);
          re.setId(rememberWord.getId());
          re.setVote(rememberWord.getVote());
          re.setCustomerId(customerId);
          this.repository.save(re);
          return rememberWordDto;
    }
    public void deleteRememberWord(String customerId,long id) {
    	 validate(customerId,id);
  
        repository.deleteById(id);
    }
    private void validate(String customerId ,long id) {

   	 Optional< RememberWord> re=this.repository.findById(id);
   	  if(re.isEmpty())throw new RuntimeException("this id is not exist");
	if (!customerId.equals(re.get().getCustomerId()))
   		throw new UnauthorizedException("you do not own this word");
    }
}
