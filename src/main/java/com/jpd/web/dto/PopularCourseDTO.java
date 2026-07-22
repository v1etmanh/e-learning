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
@Schema(name = "PopularCourseDTO", description = "Compact course card used in creator dashboards and popularity listings.")
public class PopularCourseDTO {
    @Schema(description = "Course identifier.", example = "12")
    private long courseId;
    @Schema(description = "Course title.", example = "Tieng Nhat so cap N5 - Tron bo")
    private String title;
    @Schema(description = "Number of enrolled students.", example = "4500")
    private long students;

    @Schema(description = "Average star rating.", example = "4.8", minimum = "0", maximum = "5")
    private double rating;
    @Schema(description = "Cover image URL.", example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn5-cover.jpg?alt=media")
    private String urlImg;

}