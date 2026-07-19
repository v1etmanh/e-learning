package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryImportResultDto {
    private int importedCount;
    private int skippedCount;
    private int invalidCount;
    @Default
    private List<String> skippedWords = new ArrayList<>();
    @Default
    private List<String> invalidReasons = new ArrayList<>();
}
