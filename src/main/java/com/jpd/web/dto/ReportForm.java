package com.jpd.web.dto;

import com.google.firebase.database.annotations.NotNull;
import com.jpd.web.model.ReportType;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReportForm {
	@NotBlank
	private ReportType type;
	@NotBlank
	private String detail;

	@NotNull
	private long courseId;

	
}
