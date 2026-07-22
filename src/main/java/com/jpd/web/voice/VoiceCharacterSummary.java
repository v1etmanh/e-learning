package com.jpd.web.voice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VoiceCharacterSummary", description = "A conversation partner the learner can practise speaking with.")
public record VoiceCharacterSummary(
        @Schema(description = "Character identifier, used when requesting a live token or starting a session.", example = "tanaka-sensei")
        String id,
        @Schema(description = "Name shown to the learner.", example = "Tanaka Sensei")
        String displayName,
        @Schema(description = "What practising with this character is like.", example = "Giao vien tieng Nhat kien nhan, noi cham va sua loi phat am.")
        String description,
        @Schema(description = "Grouping used to organise characters in the picker.", example = "teacher")
        String category,
        @Schema(description = "Identifier of the Gemini Live voice this character speaks with.", example = "Kore")
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
