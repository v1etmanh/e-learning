package com.jpd.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CourseDescriptionDto", description = "Full public detail page of a course: metadata, creator, curriculum, statistics and feedback.")
public class CourseDescriptionDto {
    // Course basic info
    @Schema(description = "Course identifier.", example = "12")
    private Long courseId;

    @Schema(description = "Course title.", example = "Tiếng Nhật sơ cấp N5 - Trọn bộ")
    private String name;

    @Schema(description = "Raw course description authored by the creator.",
            example = "Khóa học N5 từ con số 0, bao gồm Hiragana, Katakana, 100 Kanji cơ bản và 25 bài Minna no Nihongo.")
    private String description;

    @Schema(description = "Language taught by the course.", example = "JAPANESE")
    private Language language;

    @Schema(description = "Language the lessons are delivered in.", example = "VIETNAMESE")
    private Language teachingLanguage;

    @Schema(description = "Course price in VND. `0` means the course is free.", example = "699000")
    private long price;

    @Schema(description = "Cover image URL.",
            example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn5-cover.jpg?alt=media")
    private String urlImg;

    @Schema(description = "Date the course was created.", example = "2025-11-03")
    private LocalDate createdAt;

    @Schema(description = "Date the course content was last modified.", example = "2026-06-18")
    private LocalDate lastUpdate;

    @Schema(description = "Whether the course is published. This endpoint only ever returns published courses, so it is always `true` here.", example = "true")
    private boolean isPublic;

    @Schema(description = "Whether the course has been banned by an admin.", example = "false")
    private boolean isBan;

    @Schema(description = "How learners gain access to the course.", example = "PAID")
    private String accessMode;

    // Parsed fields
    @Schema(description = "What the learner will be able to do after finishing the course.",
            example = "Đọc viết thành thạo Hiragana/Katakana; giao tiếp cơ bản trong đời sống hằng ngày.")
    private String learningObject;

    @Schema(description = "Prerequisites the learner should meet before enrolling.",
            example = "Không yêu cầu kiến thức nền, chỉ cần máy tính hoặc điện thoại có kết nối internet.")
    private String requirements;

    @Schema(description = "Who the course is aimed at.",
            example = "Người mới bắt đầu học tiếng Nhật, sinh viên chuẩn bị thi JLPT N5.")
    private String targetAudience;

    // Creator info
    @Schema(description = "Creator who authored the course, with their aggregate teaching statistics.")
    private CreatorSimpleDto creator;

    // Chapters/Modules
    @Schema(description = "Course curriculum: chapters, each containing its modules.")
    private List<Chapter> chapters;

    // Statistics
    @Schema(description = "Total number of enrolled students.", example = "4500")
    private int totalStudents;

    @Schema(description = "Total number of feedback entries left on the course.", example = "312")
    private int totalFeedbacks;

    @Schema(description = "Average star rating across all feedback.", example = "4.8", minimum = "0", maximum = "5")
    private double averageRating;

    @Schema(description = "Total number of modules across all chapters.", example = "48")
    private int totalModules;

    // Feedback list
    @Schema(description = "Feedback left by learners on this course.")
    private List<FeedbackSimpleDto> feedbacks;
}