package com.jpd.web.voice;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VoiceLiveStartRequest", description = "Request for a short-lived token to open a Gemini Live voice session.")
public record VoiceLiveStartRequest(
        @NotBlank(message = "characterId is required")
        @Schema(description = "Which character to converse with. Must be one returned by `GET /api/voice/characters`.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "tanaka-sensei")
        String characterId
) {
}
