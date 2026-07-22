package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.jpd.web.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AdminCreatorListDto", description = "Creator row in the admin creator list.")
public class AdminCreatorListDto {
    @Schema(description = "Creator identifier - the Keycloak user id.", example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
    private String creatorId;
    @Schema(description = "Creator name.", example = "Akiko Suzuki")
    private String fullName;

    @Schema(description = "Avatar image URL.", example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Favatar.jpg?alt=media")
    private String imageUrl;
    @Schema(description = "Review state of the creator profile.", example = "SUCCESS")
    private Status status;
    @Schema(description = "Unpaid earnings balance in VND.", example = "15750000.0")
    private Double balance;
    @Schema(description = "Number of courses published.", example = "12")
    private Integer totalCourses;
    @Schema(description = "Students across all their courses.", example = "2847")
    private Integer totalStudents;
    @Schema(description = "Average rating across all their courses.", example = "4.8", minimum = "0", maximum = "5")
    private Double avgRating;
    @Schema(description = "Warnings issued by admins. Three within 90 days auto-suspends the creator.", example = "1")
    private Integer warningCount;
    @Schema(description = "When the creator profile was created.", example = "2025-11-03")
    private Date createDate;
}
