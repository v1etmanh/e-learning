package com.jpd.web.voice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VoiceLiveTokenResponse", description = "Short-lived credentials for connecting directly to Gemini Live from the client.")
public record VoiceLiveTokenResponse(
        @Schema(description = "Ephemeral Gemini Live token. Treat as a secret and do not persist it - it expires at `expiresAt`.",
                example = "auth_tokens/abc123def456")
        String accessToken,
        @Schema(description = "Gemini model the token is scoped to.", example = "gemini-2.0-flash-live-001")
        String model,
        @Schema(description = "Character the session is configured for, echoed from the request.", example = "tanaka-sensei")
        String characterId,
        @Schema(description = "When the token stops working, ISO-8601. Request a new one after this.", example = "2026-07-22T09:45:30Z")
        String expiresAt
) {
}
