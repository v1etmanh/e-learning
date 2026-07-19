package com.jpd.web.voice;

public record VoiceCharacterSummary(
        String id,
        String displayName,
        String description,
        String category,
        String liveVoice
) {
    public static VoiceCharacterSummary from(VoiceCharacterProfile profile) {
        return new VoiceCharacterSummary(
                profile.id(),
                profile.displayName(),
                profile.description(),
                profile.category(),
                profile.liveVoice()
        );
    }
}
