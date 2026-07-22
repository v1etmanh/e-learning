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
@io.swagger.v3.oas.annotations.media.Schema(name = "DictionaryImportResultDto",
        description = "Outcome of a bulk JSON dictionary import. A partially successful import still returns 200 — check the counters.")
public class DictionaryImportResultDto {

    @io.swagger.v3.oas.annotations.media.Schema(description = "Number of new entries saved.", example = "42")
    private int importedCount;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Number of entries skipped as duplicates — either already in the learner's dictionary "
            + "or repeated within the file. Comparison is on the normalised word.", example = "7")
    private int skippedCount;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Number of entries rejected as unusable.", example = "1")
    private int invalidCount;

    @Default
    @io.swagger.v3.oas.annotations.media.Schema(description = "The duplicate words that were skipped.", example = "[\"天気\", \"学校\"]")
    private List<String> skippedWords = new ArrayList<>();

    @Default
    @io.swagger.v3.oas.annotations.media.Schema(description = "Why each rejected entry was rejected.", example = "[\"Một entry không có word\"]")
    private List<String> invalidReasons = new ArrayList<>();
}
