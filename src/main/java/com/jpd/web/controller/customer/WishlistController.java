package com.jpd.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.WishlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "Courses the signed-in customer has saved for later. Read the wishlist via `GET /api/customer/learning_course_list`.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class WishlistController {
@Autowired
private WishlistService wishlistService;

@Operation(
    summary = "Add a course to the wishlist",
    description = """
        Saves a course to the signed-in customer's wishlist and increments the course's wishlist metric.

        The customer is taken from the `sub` claim of the bearer token. Adding the same course twice is
        rejected with 409. On success the response has no body.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Course added to the wishlist. Empty body.",
        content = @Content),
    @ApiResponse(responseCode = "400", description = "`courseId` was not a valid number (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "404", description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "409", description = "The course is already on this customer's wishlist.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/{courseId}")
public ResponseEntity<?> addToYourWishlist(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
		@Parameter(description = "Identifier of the course to save.", required = true, example = "12")
		@PathVariable("courseId")long courseId)
{
  this.wishlistService.addWishlist(jwt.getClaimAsString("sub"), courseId);
  return ResponseEntity.status(HttpStatus.CREATED).build();
}
}
