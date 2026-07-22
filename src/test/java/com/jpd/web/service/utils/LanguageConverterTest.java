package com.jpd.web.service.utils;

import com.jpd.web.model.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageConverterTest {

    @Test
    void toCode_roundTripsAllEnumValues() {
        assertThat(LanguageConverter.toCode(Language.VIETNAMESE)).isEqualTo("vi");
        assertThat(LanguageConverter.toCode(Language.ENGLISH)).isEqualTo("en");
        assertThat(LanguageConverter.toCode(Language.CHINESE)).isEqualTo("zh");
        assertThat(LanguageConverter.toCode(Language.JAPANESE)).isEqualTo("ja");
        assertThat(LanguageConverter.toCode(Language.KOREAN)).isEqualTo("ko");
        assertThat(LanguageConverter.toCode(Language.FRENCH)).isEqualTo("fr");
        assertThat(LanguageConverter.toCode(Language.GERMAN)).isEqualTo("de");
        assertThat(LanguageConverter.toCode(Language.SPANISH)).isEqualTo("es");
        assertThat(LanguageConverter.toCode(Language.ITALIAN)).isEqualTo("it");
        assertThat(LanguageConverter.toCode(Language.RUSSIAN)).isEqualTo("ru");
    }

    @Test
    void toCode_defaultsToVi_whenNull() {
        assertThat(LanguageConverter.toCode(null)).isEqualTo("vi");
    }

    @Test
    void fromCode_defaultsToVietnamese_whenNullOrEmpty() {
        assertThat(LanguageConverter.fromCode(null)).isEqualTo(Language.VIETNAMESE);
        assertThat(LanguageConverter.fromCode("")).isEqualTo(Language.VIETNAMESE);
    }

    @Test
    void fromCode_isCaseInsensitive_andAccepts3LetterAliases() {
        assertThat(LanguageConverter.fromCode("EN")).isEqualTo(Language.ENGLISH);
        assertThat(LanguageConverter.fromCode("eng")).isEqualTo(Language.ENGLISH);
        assertThat(LanguageConverter.fromCode("vie")).isEqualTo(Language.VIETNAMESE);
    }

    @Test
    void fromCode_unrecognized_fallsBackToVietnamese_notException() {
        assertThat(LanguageConverter.fromCode("xx-unknown")).isEqualTo(Language.VIETNAMESE);
    }

    @Test
    void toCodes_twoLanguages_dedupsWhenSecondEqualsPrimary() {
        assertThat(LanguageConverter.toCodes(Language.ENGLISH, Language.ENGLISH)).containsExactly("en");
    }

    @Test
    void toCodes_twoLanguages_secondNull_returnsSingleElementList() {
        assertThat(LanguageConverter.toCodes(Language.ENGLISH, null)).containsExactly("en");
    }

    @Test
    void toCodes_twoLanguages_bothNull_fallsBackToVi() {
        assertThat(LanguageConverter.toCodes((Language) null, (Language) null)).containsExactly("vi");
    }

    @Test
    void isValidCode_checksMembershipInSupportedSet() {
        assertThat(LanguageConverter.isValidCode("en")).isTrue();
        assertThat(LanguageConverter.isValidCode("xx")).isFalse();
        assertThat(LanguageConverter.isValidCode(null)).isFalse();
    }

    @Test
    void getAllCodes_returnsAllTenSupportedCodes() {
        assertThat(LanguageConverter.getAllCodes()).containsExactlyInAnyOrder(
                "vi", "en", "zh", "ja", "ko", "fr", "de", "es", "it", "ru");
    }
}
