package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

import com.jpd.web.model.AccessMode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Schema(name = "CourseCardDto", description = "Summary card of a course in the creator's own course list. Includes the join key, so it is only ever returned to the owning creator.")
public class CourseCardDto {
/*
 * id: 1,
      name: "Public Course A",
      image: "https://via.placeholder.com/150",
      createdDate: "2024-09-20",
      studentCount: 120,
      reviewCount: 40,
      rating: 4.5,*/
	@Schema(description = "Course identifier.", example = "12")
	private long id ;
	@Schema(description = "Course title.", example = "Tieng Nhat so cap N5 - Tron bo")
	private String name;
	@Schema(description = "Date the course was created.", example = "2025-11-03")
	private LocalDate createdDate;
	@Schema(description = "Number of enrolled students.", example = "120")
	private int studentCount;
	@Schema(description = "Number of reviews left on the course.", example = "40")
	private double reviewCount;

	@Schema(description = "Average star rating.", example = "4.5", minimum = "0", maximum = "5")
	private double rating;
	@Schema(description = "Cover image URL.", example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn5-cover.jpg?alt=media")
	private String image;
	@Schema(description = "Whether enrollment is open or gated by a join key.", example = "PUBLIC")
	private AccessMode type;
	@Schema(description = "Whether the course is published and visible in the catalogue.", example = "true")
	private boolean isPublic;
	@Schema(description = "Key learners must supply to enroll in a `PRIVATE` course. Meaningless for `PUBLIC` courses.", example = "N5-2026-SPRING")
	private String joinKey;
}
