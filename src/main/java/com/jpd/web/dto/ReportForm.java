package com.jpd.web.dto;

import com.google.firebase.database.annotations.NotNull;
import com.jpd.web.model.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(name = "ReportForm", description = "A learner's report against a course they created or are enrolled in. "
		+ "Note: the constraint annotations on these fields are not enforced — the controller does not apply `@Valid`.")
public class ReportForm {

	@NotBlank
	@Schema(description = "Reason for the report.", example = "MISLEADING_INFORMATION")
	private ReportType type;

	@NotBlank
	@Schema(description = "Free-text explanation of what is wrong with the course.",
			example = "Khóa học quảng cáo có 50 bài nhưng thực tế chỉ có 12 bài, nội dung không đúng mô tả.")
	private String detail;

	@NotNull
	@Schema(description = "Identifier of the course being reported. The caller must be enrolled in it, or be its creator.",
			example = "12")
	private long courseId;

	
}
