package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Language;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Schema(name = "CourseFormDto", description = "Multipart form for creating a course. The new course always starts unpublished.")
public class CourseFormDto {

    @NotBlank(message = "Course name is required")
    @Size(max = 255, message = "Course name must be less than 255 characters")
    @Schema(description = "Course title.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255, example = "Tieng Nhat so cap N5 - Tron bo")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description is too long")
    @Schema(description = "Full course description.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 5000, example = "Khoa hoc N5 tu con so 0, bao gom Hiragana, Katakana va 25 bai Minna no Nihongo.")
    private String description;

    @NotBlank(message = "Target audience is required")
    @Size(max = 1000, message = "Target audience is too long")
    @Schema(description = "Who the course is for.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 1000, example = "Nguoi moi bat dau hoc tieng Nhat.")
    private String targetAudience;

    @Size(max = 2000, message = "Requirements are too long")
    @Schema(description = "Prerequisites for enrolling.", maxLength = 2000, example = "Khong yeu cau kien thuc nen.")
    private String requirements;

    @Size(max = 2000, message = "Learning objectives are too long")
    @Schema(description = "What the learner will be able to do afterwards.", maxLength = 2000, example = "Doc viet thanh thao Hiragana va Katakana.")
    private String learningObject;

    @NotNull
    @Schema(description = "Language taught by the course.", requiredMode = Schema.RequiredMode.REQUIRED, example = "JAPANESE")
    private Language language;
    @NotNull
    @Schema(description = "Language the lessons are delivered in.", requiredMode = Schema.RequiredMode.REQUIRED, example = "VIETNAMESE")
    private Language teachingLanguage;
    @PositiveOrZero(message = "Price must be zero or positive")
    @Schema(description = "Price in VND. Zero means free.", minimum = "0", example = "699000")
    private long price;

   
    @Schema(description = "Cover image. Optional - uploaded to Firebase Storage when present.", type = "string", format = "binary")
    private MultipartFile imgFile;

    @NotNull(message = "Course type is required")
    @Schema(description = "Whether enrollment is open or gated by a join key.", requiredMode = Schema.RequiredMode.REQUIRED, example = "PUBLIC")
    private AccessMode accessMode;
}
