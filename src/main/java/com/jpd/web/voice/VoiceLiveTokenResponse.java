package com.jpd.web.voice;

public record VoiceLiveTokenResponse(
        String accessToken,
        String model,
        String characterId,
        String expiresAt
) {
}
