package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreatorSimpleDto", description = "Public snapshot of a creator shown alongside their courses.")
public class CreatorSimpleDto {

    @Schema(description = "Creator identifier — the Keycloak user id (`sub` claim).", example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
    private String creatorId;

    @Schema(description = "Creator's full name.", example = "Akiko Suzuki")
    private String fullName;

    @Schema(description = "Short self-description / headline.", example = "Giáo viên tiếng Nhật 5 năm kinh nghiệm, chuyên luyện thi JLPT N3-N1.")
    private String titleSelf;

    @Schema(description = "Avatar image URL.",
            example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2F7f3c1a90-avatar.jpg?alt=media")
    private String imageUrl;

    @Schema(description = "PayPal account used for creator payouts.", example = "akiko.suzuki@example.com")
    private String paymentEmail;

    @Schema(description = "Number of courses published by this creator.", example = "12")
    private int totalCourses;

    @Schema(description = "Total students enrolled across all of this creator's courses.", example = "2847")
    private int totalStudents;

    @Schema(description = "Average star rating across all of this creator's courses.", example = "4.8", minimum = "0", maximum = "5")
    private double averageRating;
}