package com.jpd.web.dto;

import com.jpd.web.model.Language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CourseInfDto", description = "Public summary card for a course, used in catalogue listings and search results.")
public class CourseInfDto {
	/* id: 5,
     name: 'Japanese Conversation Mastery',
     img: 'https://dichthuattiengnhatban.com/wp-content/uploads/2024/04/App-hc-tieng-nhat-N3-1-300x300.jpg',
     numberStudent: 4500,
     rating: 4.8,
     instructor: 'Akiko Suzuki',
     price: 699000*/
	@Schema(description = "Course identifier.", example = "5")
	private long id;

	@Schema(description = "Course title.", example = "Japanese Conversation Mastery")
	private String name;

	@Schema(description = "Cover image URL.", example = "https://dichthuattiengnhatban.com/wp-content/uploads/2024/04/App-hc-tieng-nhat-N3-1-300x300.jpg")
	private String img;

	@Schema(description = "Number of students enrolled, taken from the course metrics.", example = "4500")
	private int numberStudent;

	@Schema(description = "Average star rating from customer feedback.", example = "4.8", minimum = "0", maximum = "5")
	private double rating;

	@Schema(description = "Full name of the creator teaching the course.", example = "Akiko Suzuki")
	private String instructor;

	@Schema(description = "Course price in VND. `0` means the course is free.", example = "699000")
	private long price;

	@Schema(description = "Language taught by the course.", example = "JAPANESE")
	private Language language;
}
