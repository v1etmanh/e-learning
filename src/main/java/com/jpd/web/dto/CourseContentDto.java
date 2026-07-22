package com.jpd.web.dto;

import java.util.List;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Schema(name = "CourseContentDto", description = "Curriculum view of a course for someone entitled to study it: the chapter/module tree "
		+ "with each module's available content types resolved.")
public class CourseContentDto {
/* course name 
 * 
 * list chapterdto
 * name 
 * ispublic
 * List<Chapter>
 * */
	@Schema(description = "Course title.", example = "Tiếng Nhật sơ cấp N5 - Trọn bộ")
	private String name;

	@Schema(description = "Whether the course is published. A creator viewing their own course may see `false` here; "
			+ "an enrolled learner only ever sees `true`.", example = "true")
	private boolean isPublic;

	@Schema(description = "Language taught by the course.", example = "JAPANESE")
	private Language language;

	@Schema(description = "Language the lessons are delivered in.", example = "VIETNAMESE")
	private Language teachingLanguage;

	@Schema(description = "Chapters in order, each with its modules. Every module carries the set of `TypeOfContent` it holds.")
	private List<Chapter>chapters;
}
