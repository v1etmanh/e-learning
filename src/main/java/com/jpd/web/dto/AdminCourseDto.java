package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import com.jpd.web.model.Language;
import com.jpd.web.model.Report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AdminCourseDto", description = "Course row in the admin moderation list, including its report count.")
public class AdminCourseDto {
	@Schema(description = "Course identifier.", example = "12")
	private Long courseId;
	@Schema(description = "Course title.", example = "Tieng Nhat so cap N5 - Tron bo")
	private String name;
	@Schema(description = "Name of the creator who authored the course.", example = "Akiko Suzuki")
	private String creatorName;
	@Schema(description = "Numeric creator reference. Note creator ids are Keycloak UUID strings elsewhere in the API.", example = "0")
	private long creatorId;
	@Schema(description = "Whether an admin has banned the course.", example = "false")
	private boolean isBan;

	@Schema(description = "Language taught by the course.", example = "JAPANESE")
	private Language language;

	@Schema(description = "How many reports learners have filed against the course. The moderation signal to sort on.", example = "3")
	private int  numberReports;
	@Schema(description = "Number of enrolled students.", example = "4500")
	private int numberStudent;
	
	@Schema(description = "Average star rating.", example = "4.8", minimum = "0", maximum = "5")
	private double avtRating;
}