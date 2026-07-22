package com.jpd.web.dto;




import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
@Schema(name = "WishlistDto", description = "Card for one course saved to the customer's wishlist.")
public class WishlistDto {

@Schema(description = "Identifier of the wishlisted course.", example = "41")
private long courseId;

@Schema(description = "Course title.", example = "Kanji 500 chữ thông dụng")
private String course_name;

@Schema(description = "Cover image URL of the course.",
		example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fkanji-500.jpg?alt=media")
private String course_img;

}
