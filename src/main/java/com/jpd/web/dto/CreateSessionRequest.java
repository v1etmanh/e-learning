package com.jpd.web.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateSessionRequest", description = "Request to open a live quiz session from one of the creator's Kahoots.")
public class CreateSessionRequest {
    @Schema(description = "Identifier of the Kahoot to run. Must belong to the caller and contain at least one MULTIPLE_CHOICE or GAPFILL question.", example = "77")
    private Long kahootId;
  
    @Schema(description = "Display name shown to participants as the host.", example = "Co Thuy")
    private String teacherName;
}