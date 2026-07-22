package com.jpd.web.voice;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VoiceDailyProgressResponse", description = "The learner's speaking practice for today, measured against their daily targets.")
public record VoiceDailyProgressResponse(
        @Schema(description = "The day being reported - today on the server.", example = "2026-07-22")
        LocalDate date,
        @Schema(description = "Completed conversations today.", example = "3")
        int conversationCount,
        @Schema(description = "Distinct characters practised with today. This is what `charactersTarget` is measured against.", example = "2")
        int uniqueCharactersCount,
        @Schema(description = "Total speaking time today, in seconds.", example = "742")
        long totalSeconds,
        @Schema(description = "Total speaking time today, in whole minutes.", example = "12")
        int totalMinutes,
        @Schema(description = "How many distinct characters the learner aims to practise with each day.", example = "3")
        int charactersTarget,
        @Schema(description = "How many minutes the learner aims to speak each day.", example = "15")
        int minutesTarget,
        @Schema(description = "Progress towards the character target, as a fraction.", example = "0.67", minimum = "0")
        double charactersProgress,
        @Schema(description = "Progress towards the minutes target, as a fraction.", example = "0.8", minimum = "0")
        double minutesProgress,
        @Schema(description = "Whether both daily targets have been met.", example = "false")
        boolean completed
) {
}
