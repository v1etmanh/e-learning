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
public class DictionaryImportFileDto {
    private String schema;
    private Integer version;
    private String exportedAt;
    private Integer rangeDays;
    private String startDate;
    private String endDate;
    private List<DictionaryImportEntryDto> entries = new ArrayList<>();
}
