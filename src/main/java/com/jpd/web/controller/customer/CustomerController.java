package com.jpd.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.CreatorDto;
import com.jpd.web.dto.CreatorProfileDto;
import com.jpd.web.dto.LearningListDto;
import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/customer")
@Tag(name = "Customer", description = "Self-service endpoints for the signed-in customer: applying to become a creator and listing their own learning content.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class CustomerController {
@Autowired
private CustomerService customerSer;

@Operation(
    summary = "Apply to become a creator",
    description = """
        Registers a creator profile for the signed-in customer and submits it for review.

        The customer is identified from the bearer token only — `sub` becomes the creator id and
        `email` is used for the confirmation notice. Neither value is accepted from the request body.

        On success the profile is stored with status `PENDING`, the customer is granted the creator
        client role in Keycloak, and a notification email is sent. If `profileImage` is present it is
        uploaded to Firebase Storage and the resulting public URL is returned as `imgUrl`.

        A customer may only apply once: a second call while a profile exists is rejected with 409.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Creator profile created and submitted for review.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CreatorDto.class),
            examples = @ExampleObject(name = "created", value = """
                {
                  "fullName": "Nguyễn Thị Thuỷ",
                  "phone": "0912345678",
                  "bio": "Giáo viên tiếng Nhật 5 năm kinh nghiệm, chuyên luyện thi JLPT N3-N1.",
                  "imgUrl": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2F7f3c1a90-avatar.jpg?alt=media",
                  "certificateUrl": null,
                  "paypalEmail": null,
                  "status": "PENDING"
                }"""))),
    @ApiResponse(responseCode = "400", description = "Uploading the profile image to Firebase failed (`code: API_ERROR`).",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token. Returned by the resource server before the controller is reached; the body is empty and a `WWW-Authenticate` header is set.",
        content = @Content),
    @ApiResponse(responseCode = "409", description = "This customer already has a creator profile.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "duplicate", value = """
                {
                  "code": "CREATOR_ALREADY_EXISTS",
                  "message": "You already have a creator profile",
                  "userMessage": "Dữ liệu đã tồn tại hoặc bị trùng lặp",
                  "path": "/api/customer/upload_profile",
                  "timestamp": "2026-07-22T09:15:30.412",
                  "details": null,
                  "traceId": "0b6f2c1e-9a44-4f0d-9d6e-2f1c8b7a1234",
                  "success": false,
                  "status": 0,
                  "responseType": null
                }"""))),
    // Known gap: @Valid on @ModelAttribute raises BindException, which GlobalExceptionHandler
    // does not map (it only handles the MethodArgumentNotValidException subclass), so constraint
    // violations currently fall through to the generic 500 handler rather than returning 400.
    @ApiResponse(responseCode = "500", description = "Bean Validation failure on the form fields, or any other unexpected server error.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "validationFailure", value = """
                {
                  "code": "INTERNAL_ERROR",
                  "message": "Validation failed for argument [0] in public org.springframework.http.ResponseEntity<com.jpd.web.dto.CreatorDto> com.jpd.web.controller.customer.CustomerController.updateProfile(...): [Field error in object 'creatorProfileDto' on field 'phone': rejected value [12ab]; default message [Số điện thoại không hợp lệ]]",
                  "userMessage": "Có lỗi xảy ra trên server, vui lòng thử lại sau",
                  "path": "/api/customer/upload_profile",
                  "timestamp": "2026-07-22T09:15:30.412",
                  "details": null,
                  "traceId": "3d9a71b2-55c4-41ab-8f10-6ce0d4a9e777",
                  "success": false,
                  "status": 0,
                  "responseType": null
                }""")))
})
@PostMapping(value="/upload_profile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<CreatorDto> updateProfile( @Valid @ModelAttribute CreatorProfileDto creatorProfileDto,@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
	String email=jwt.getClaimAsString("email");
	String customerId=jwt.getClaimAsString("sub");
 CreatorDto crdto=   this.customerSer.uploadProfile(email,customerId, creatorProfileDto);
  return ResponseEntity.status(HttpStatus.CREATED).body(crdto);




}

@Operation(
    summary = "List the customer's enrolled courses and wishlist",
    description = """
        Returns everything the signed-in customer is learning: enrolled courses with their completion
        progress, plus the courses saved to their wishlist.

        The customer is identified from the `sub` claim of the bearer token. Both lists are returned in
        one payload and are empty arrays when the customer has no enrollments or no wishlist entries.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Enrolled courses and wishlist for this customer.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = LearningListDto.class),
            examples = @ExampleObject(name = "learningList", value = """
                {
                  "cardDtos": [
                    {
                      "courseId": 12,
                      "course_name": "Tiếng Nhật sơ cấp N5 - Trọn bộ",
                      "course_img": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn5-cover.jpg?alt=media",
                      "progress": 65.5
                    },
                    {
                      "courseId": 27,
                      "course_name": "Luyện thi JLPT N3 - Ngữ pháp",
                      "course_img": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn3-grammar.jpg?alt=media",
                      "progress": 0.0
                    }
                  ],
                  "wishlistDtos": [
                    {
                      "courseId": 41,
                      "course_name": "Kanji 500 chữ thông dụng",
                      "course_img": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fkanji-500.jpg?alt=media"
                    }
                  ]
                }"""))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token. Returned by the resource server before the controller is reached; the body is empty and a `WWW-Authenticate` header is set.",
        content = @Content),
    @ApiResponse(responseCode = "500", description = "Unexpected server error while loading enrollments or wishlist.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/learning_course_list")
public ResponseEntity<LearningListDto> retrieveYourCourses(@Parameter(hidden = true) @AuthenticationPrincipal
		Jwt jwt){
	String customerId=jwt.getClaimAsString("sub");
	LearningListDto res=this.customerSer.retrieveLearningList(customerId);
	return ResponseEntity.status(HttpStatus.OK).body(res);
}

}
