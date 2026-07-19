package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagicDiaryScores {
    private double contentAccuracy;
    private double grammar;
    private double vocabulary;
    private String feedback;
    private String nextStep;
}
