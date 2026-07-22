package com.jpd.web.controller.admin;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.AdminCourseDetailDto;
import com.jpd.web.dto.AdminCourseDto;
import com.jpd.web.dto.ApiResponse;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.AdminCourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - courses", description = """
        Course moderation. Every endpoint under `/api/admin/**` requires the `ADMIN` realm role; a valid
        token without it is rejected with 403.

        Responses are wrapped in the `ApiResponse` envelope: the payload sits under `data`.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    // Lấy danh sách tất cả khóa học có phân trang
    @Operation(
        summary = "List all courses",
        description = """
            Returns every course on the platform, page by page, with its report count, student count and
            rating — the moderation queue view. Unlike the public catalogue this includes unpublished and
            banned courses.

            When `search` is given, it is matched case-insensitively against course name and description.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "One page of courses, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "coursePage", value = """
                    {
                      "success": true,
                      "data": {
                        "content": [
                          {
                            "courseId": 12,
                            "name": "Tieng Nhat so cap N5 - Tron bo",
                            "creatorName": "Akiko Suzuki",
                            "creatorId": 0,
                            "isBan": false,
                            "language": "JAPANESE",
                            "numberReports": 3,
                            "numberStudent": 4500,
                            "avtRating": 4.8
                          }
                        ],
                        "totalElements": 137,
                        "totalPages": 7,
                        "number": 0,
                        "size": 20
                      },
                      "error": null,
                      "timestamp": "2026-07-22T09:15:30.412"
                    }"""))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "`page` or `size` was outside its allowed range, or was not a valid integer.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500",
            description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCourseDto>>> getAllCourses(
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Courses per page, 1 to 100.", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Optional keyword matched case-insensitively against course name and description.", example = "tieng nhat")
            @RequestParam(required = false) String search
    ) {
        log.info("Admin fetching courses. page={}, size={}, search={}", page, size, search);
        Page<AdminCourseDto> courses = adminCourseService.getAllCourses(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    // Lấy chi tiết khóa học theo ID
    @Operation(
        summary = "Get full course detail for moderation",
        description = """
            Returns everything the public detail page shows plus the reports filed against the course, so
            an admin can judge a complaint against the actual content.

            Unlike the public endpoint this works for unpublished and banned courses.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Course detail with reports, wrapped in the `ApiResponse` envelope.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminCourseDetailDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "`courseId` was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500",
            description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<AdminCourseDetailDto>> getCourseById(
            @Parameter(description = "Identifier of the course to inspect.", required = true, example = "12")
            @PathVariable Long courseId) {
    	AdminCourseDetailDto course = adminCourseService.getCourseById(courseId);
        return ResponseEntity.ok(ApiResponse.success(course));
    }

    // Khóa khóa học
    @Operation(
        summary = "Ban a course",
        description = """
            Flags a course as banned, taking it out of circulation. Reversible via the unban endpoint.

            Banning does not change the course's published flag — it sets the ban flag only.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Course banned.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "banned", value = """
                    {
                      "success": true,
                      "data": "Course banned successfully",
                      "error": null,
                      "timestamp": "2026-07-22T09:15:30.412"
                    }"""))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "`courseId` was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500",
            description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{courseId}/ban")
    public ResponseEntity<ApiResponse<String>> banCourse(
            @Parameter(description = "Identifier of the course to ban.", required = true, example = "12")
            @PathVariable Long courseId) {
        adminCourseService.banCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Course banned successfully"));
    }

    // Mở khóa khóa học
    @Operation(
        summary = "Unban a course",
        description = "Clears the ban flag on a course, restoring it. Its published flag is unchanged, so a course that was unpublished stays unpublished.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Course unbanned.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "unbanned", value = """
                    {
                      "success": true,
                      "data": "Course unbanned successfully",
                      "error": null,
                      "timestamp": "2026-07-22T09:15:30.412"
                    }"""))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "`courseId` was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing, expired or invalid bearer token.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "The token does not carry the `ADMIN` role.", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "No course exists with this id (`code: COURSE_NOT_FOUND`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500",
            description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{courseId}/unban")
    public ResponseEntity<ApiResponse<String>> unbanCourse(
            @Parameter(description = "Identifier of the course to unban.", required = true, example = "12")
            @PathVariable Long courseId) {
        adminCourseService.unbanCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Course unbanned successfully"));
    }

    // Thay đổi trạng thái public/private

}