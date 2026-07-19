package com.jpd.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DictionaryImportEntryDto {
    private String word;
    private String meaning;
    private String description;
    private List<String> synonyms = new ArrayList<>();
    private List<String> example = new ArrayList<>();
    private String language;
    private String context;
    private String sourceUrl;
    private String sourceTitle;
    private String capturedAt;
}
