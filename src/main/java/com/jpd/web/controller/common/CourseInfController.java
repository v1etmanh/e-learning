package com.jpd.web.controller.common;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.CourseDescriptionDto;
import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.CourseInfService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/course")
@Tag(name = "Course catalogue", description = "Public course browsing: recommendations, search and course detail. No authentication required.")
public class CourseInfController
{
	@Autowired
	private CourseInfService courseInfService;

@Operation(
    summary = "List recommended courses",
    description = """
        Returns up to three published courses per distinct course language, ranked by a blended score of
        `averageRating * 0.7 + totalStudents * 0.3`. Unpublished courses are excluded.

        Takes no parameters and is not personalised — every caller gets the same list. Public endpoint.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Recommended courses, grouped by language in the order the languages occur in the catalogue.",
        content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = CourseInfDto.class)),
            examples = @ExampleObject(name = "recommendations", value = """
                [
                  {
                    "id": 5,
                    "name": "Japanese Conversation Mastery",
                    "img": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fjp-conversation.jpg?alt=media",
                    "numberStudent": 4500,
                    "rating": 4.8,
                    "instructor": "Akiko Suzuki",
                    "price": 699000,
                    "language": "JAPANESE"
                  },
                  {
                    "id": 18,
                    "name": "Business English for IT",
                    "img": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fbiz-english.jpg?alt=media",
                    "numberStudent": 2130,
                    "rating": 4.6,
                    "instructor": "Mark Peterson",
                    "price": 0,
                    "language": "ENGLISH"
                  }
                ]"""))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error while building the recommendation list.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/recommend_courses")
public ResponseEntity<List<CourseInfDto>> getMethodName() {
    return ResponseEntity.ok(courseInfService.getRecommendCourses());
}

@Operation(
    summary = "Search courses by keyword",
    description = """
        Full-text search over the course catalogue, returned page by page. Results are not sorted —
        they come back in repository order.

        The response is a flat JSON object rather than a Spring `Page`: the course list is under
        `content`, with paging metadata alongside it. Public endpoint.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "One page of matching courses plus paging metadata. `content` is empty when nothing matches.",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "searchPage", value = """
                {
                  "content": [
                    {
                      "id": 5,
                      "name": "Japanese Conversation Mastery",
                      "img": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fjp-conversation.jpg?alt=media",
                      "numberStudent": 4500,
                      "rating": 4.8,
                      "instructor": "Akiko Suzuki",
                      "price": 699000,
                      "language": "JAPANESE"
                    }
                  ],
                  "currentPage": 0,
                  "totalPages": 3,
                  "totalElements": 27,
                  "size": 10,
                  "hasNext": true,
                  "hasPrevious": false
                }"""))),
    @ApiResponse(responseCode = "400", description = "`page` or `size` was not a valid integer (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "`name` was omitted, `size` was zero or negative, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/search")
public ResponseEntity<?> searchCourses(
        @io.swagger.v3.oas.annotations.Parameter(description = "Search keyword matched against course fields. Surrounding whitespace is trimmed. Required.",
                required = true, example = "tiếng nhật")
        @RequestParam String name,
        @io.swagger.v3.oas.annotations.Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @io.swagger.v3.oas.annotations.Parameter(description = "Number of courses per page. Must be greater than zero.", example = "10")
        @RequestParam(defaultValue = "10") int size
) {
    Page<CourseInfDto> result = this.courseInfService.searchByKey(name, page, size);

    return ResponseEntity.ok(Map.of(
        "content", result.getContent(),
        "currentPage", result.getNumber(),
        "totalPages", result.getTotalPages(),
        "totalElements", result.getTotalElements(),
        "size", result.getSize(),
        "hasNext", result.hasNext(),
        "hasPrevious", result.hasPrevious()
    ));
}

@Operation(
    summary = "Get full course detail",
    description = """
        Returns the public detail page of a single published course: metadata, creator profile,
        chapter/module curriculum, aggregate statistics and learner feedback. Public endpoint.

        Unpublished courses are treated as inaccessible and answered with 401, not 404.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Course detail.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDescriptionDto.class))),
    @ApiResponse(responseCode = "400", description = "`id` was not a valid number (`code: TYPE_MISMATCH`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "The course exists but is not published.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "notPublished", value = """
                {
                  "code": "UNAUTHORIZED",
                  "message": "this course is not exist",
                  "userMessage": "Bạn không có quyền truy cập",
                  "path": "/api/course/12",
                  "timestamp": "2026-07-22T09:15:30.412",
                  "details": null,
                  "traceId": "0b6f2c1e-9a44-4f0d-9d6e-2f1c8b7a1234",
                  "success": false,
                  "status": 0,
                  "responseType": null
                }"""))),
    // Known gap: the service throws a plain RuntimeException for a missing course, so an unknown id
    // is reported as 500 rather than the 404 that CourseNotFoundException would produce.
    @ApiResponse(responseCode = "500", description = "No course exists with this id, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/{id}")
public ResponseEntity<?>retrieveCourseDetail(
		@io.swagger.v3.oas.annotations.Parameter(description = "Identifier of the course to load.", required = true, example = "12")
		@PathVariable("id") long id){

	return ResponseEntity.ok( this.courseInfService.getCourseDescription(id));
}


}
