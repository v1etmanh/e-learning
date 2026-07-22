package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import com.jpd.web.model.ModuleContent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(name = "ModuleContentUpdateResult", description = "Outcome of a content save: a partial success is normal - items that fail moderation are listed in `rejected` while the rest are stored.")
public class ModuleContentUpdateResult {
	  @Schema(description = "Items that passed moderation and were saved.")
	  private List<ModuleContent> approved;
	    @Schema(description = "Items that were refused, each with the reason.")
	    private List<RejectedContent> rejected;
	    
}
