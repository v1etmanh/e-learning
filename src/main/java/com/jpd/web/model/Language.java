package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Language", description = "Natural language of a course. Used both for the language being taught "
        + "and, on `CourseDescriptionDto.teachingLanguage`, for the language of instruction.")
public enum Language {ENGLISH,VIETNAMESE,
CHINESE,JAPANESE,KOREAN,FRENCH,GERMAN,SPANISH,ITALIAN,RUSSIAN,
}
