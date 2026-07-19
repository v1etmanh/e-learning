package com.jpd.web.voice;

/**
 * A safe, product-facing character profile for the speaking companion.
 * The system instruction is kept server-side and is never returned by the API.
 */
public record VoiceCharacterProfile(
        String id,
        String displayName,
        String description,
        String category,
        String liveVoice,
        String systemInstruction
) {
}
