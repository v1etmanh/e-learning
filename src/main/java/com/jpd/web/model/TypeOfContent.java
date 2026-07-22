package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TypeOfContent", description = """
        Kind of learning content held by a module. A single module may carry several types at once.

        * `FLASHCARD` — vocabulary flashcards
        * `MULTIPLE_CHOICE` — multiple-choice questions
        * `GAPFILL` — fill-in-the-blank exercises
        * `WRITING` — free writing tasks
        * `SPEAKING_PASSAGE` — read-aloud speaking practice
        * `SPEAKING_PICTURE` — describe-the-picture speaking practice
        * `VIDEO` — video lesson
        * `LISTEN_CHOICE` — listening comprehension with multiple choice
        * `READING` — reading passage
        * `PDF` — downloadable PDF material
        """)
public enum TypeOfContent {
    FLASHCARD,
    MULTIPLE_CHOICE,
    GAPFILL, 
    WRITING,
    SPEAKING_PASSAGE,
    SPEAKING_PICTURE,
    VIDEO,
    LISTEN_CHOICE,
    READING,
    PDF
}
