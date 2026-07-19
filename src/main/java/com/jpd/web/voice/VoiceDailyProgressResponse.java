package com.jpd.web.voice;

import java.time.LocalDate;

public record VoiceDailyProgressResponse(
        LocalDate date,
        int conversationCount,
        int uniqueCharactersCount,
        long totalSeconds,
        int totalMinutes,
        int charactersTarget,
        int minutesTarget,
        double charactersProgress,
        double minutesProgress,
        boolean completed
) {
}
