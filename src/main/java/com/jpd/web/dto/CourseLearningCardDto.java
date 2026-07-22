package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(name = "CourseLearningCardDto", description = "Card for one enrolled course, showing how far the customer has progressed through it.")
public class CourseLearningCardDto {
	/* courseId: 1,
     course_name: "Complete React Developer Course",
     course_img: "https://img-c.udemycdn.com/course/240x135/851712_fc61_6.jpg",
     progress: 65*/
	@Schema(description = "Identifier of the enrolled course.", example = "12")
	private long courseId;

	@Schema(description = "Course title.", example = "Tiếng Nhật sơ cấp N5 - Trọn bộ")
	private String course_name;

	@Schema(description = "Cover image URL of the course.",
			example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn5-cover.jpg?alt=media")
	private String course_img;

	@Schema(description = "Completion percentage: module contents the customer has finished divided by the course total, times 100.",
			example = "65.5", minimum = "0", maximum = "100")
	private double progress;
}
