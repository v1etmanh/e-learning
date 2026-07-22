package com.jpd.web.dto;

import java.sql.Date;
import java.util.List;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
@Schema(name = "LearningListDto", description = "Everything the signed-in customer is learning: enrolled courses plus saved wishlist entries.")
public class LearningListDto {

@Schema(description = "Courses the customer is enrolled in, each with completion progress. Empty when there are no enrollments.")
private List<CourseLearningCardDto>cardDtos;

@Schema(description = "Courses the customer saved to their wishlist. Empty when the wishlist is empty.")
private List<WishlistDto>wishlistDtos;
}
