package com.jpd.web.voice;

import jakarta.validation.constraints.NotBlank;

public record VoiceLiveStartRequest(
        @NotBlank(message = "characterId is required") String characterId
) {
}
