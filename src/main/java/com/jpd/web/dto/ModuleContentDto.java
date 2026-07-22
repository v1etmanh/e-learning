package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import com.jpd.web.model.ModuleContent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Schema(name = "ModuleContentDto", description = "A module and the full set of content items to store in it.")
public class ModuleContentDto {
@Schema(description = "Identifier of the module being filled. Must belong to a course the caller owns.", example = "56")
private long moduleId;
@Schema(description = "The content items to save. Text-bearing items are screened by content moderation before being stored.")
private List<ModuleContent> moduleContent;
}
