package com.jpd.web.voice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoiceCharacterCatalogTest {

    private final VoiceCharacterCatalog catalog = new VoiceCharacterCatalog();

    @Test
    void all_returnsAllTwentyRegisteredProfiles() {
        assertThat(catalog.all()).hasSize(20);
    }

    @Test
    void get_returnsKnownProfile_withExactDisplayName() {
        assertThat(catalog.get("dog").displayName()).isEqualTo("Pip the Dog");
        assertThat(catalog.get("wizard").displayName()).isEqualTo("Aldric the Wizard");
    }

    @Test
    void get_returnsNull_forUnknownId_notAnException() {
        assertThat(catalog.get("nonexistent-id")).isNull();
    }
}
