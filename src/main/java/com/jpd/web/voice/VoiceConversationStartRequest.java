package com.jpd.web.voice;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VoiceConversationStartRequest", description = "Request to begin tracking a practice conversation towards the learner's daily goal.")
public record VoiceConversationStartRequest(
        @NotBlank(message = "characterId is required")
        @Schema(description = "Which character the learner is practising with.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "tanaka-sensei")
        String characterId
) {
}
