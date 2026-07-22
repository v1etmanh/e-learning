package com.jpd.web.controller.admin;

import com.jpd.web.dto.*;
import com.jpd.web.model.AuditLog;
import com.jpd.web.model.Report;
import com.jpd.web.model.Status;
import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.AdminCreatorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/creators")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - creators", description = """
        Creator vetting and moderation: certificate review, warnings, bans and the audit trail.

        Every endpoint under `/api/admin/**` requires the `ADMIN` realm role; a valid token without it is
        rejected with 403. The acting admin is identified from the `email` claim of the bearer token and
        recorded on every audit entry.

        Note: creator lookups in this controller use `orElse(null)`, so passing a `creatorId` that does
        not exist produces a NullPointerException and a 500 rather than a 404.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class AdminCreatorController {

    private final AdminCreatorService adminCreatorService;

    // List creators
    @Operation(
        summary = "List creators",
        description = """
            Returns creators page by page, optionally filtered by review `status` and a search keyword,
            each with their balance, course and student counts, rating and warning count.

            Use `status=PENDING` to find applications awaiting review.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "One page of creators.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "creatorPage", value = """
                    {
                      "content": [
                        {
                          "creatorId": "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f",
                          "fullName": "Akiko Suzuki",
                          "imageUrl": "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Favatar.jpg?alt=media",
                          "status": "SUCCESS",
                          "balance": 15750000.0,
                          "totalCourses": 12,
                          "totalStudents": 2847,
                          "avgRating": 4.8,
                          "warningCount": 1,
                          "createDate": "2025-11-03"
                        }
                      ],
                      "totalElements": 58,
                      "totalPages": 3,
                      "number": 0,
                      "size": 20
                    }"""))),
        @ApiResponse(responseCode = "400", description = "`status` is not one of the allowed values, or a paging parameter was not an integer (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<AdminCreatorListDto>> getCreatorList(
            @Parameter(description = "Only return creators in this review state. Omit for all.", example = "PENDING")
            @RequestParam(required = false) Status status,
            @Parameter(description = "Optional keyword matched against the creator's details.", example = "Suzuki")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Creators per page.", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminCreatorListDto> result = adminCreatorService.getCreatorList(status, search, page, size);
        return ResponseEntity.ok(result);
    }

    // Get creator detail
    @Operation(
        summary = "Get a creator's full moderation record",
        description = "Returns a creator's profile, certificates, financials, ban state, recent reports and recent courses — everything needed to decide on an action.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The creator's moderation record.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminCreatorDetailDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No creator exists with this id, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{creatorId}")
    public ResponseEntity<AdminCreatorDetailDto> getCreatorDetail(
            @Parameter(description = "Keycloak user id of the creator.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId) {
        AdminCreatorDetailDto dto = adminCreatorService.getCreatorDetail(creatorId);
        return ResponseEntity.ok(dto);
    }

    // Get pending certificates
    @Operation(
        summary = "List creator applications awaiting certificate review",
        description = "Returns every creator whose status is `PENDING`, with their uploaded qualification documents — the certificate review queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Applications awaiting review. Empty when the queue is clear.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = CertificateApprovalDto.class)))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/pending-certificates")
    public ResponseEntity<List<CertificateApprovalDto>> getPendingCertificates() {
        List<CertificateApprovalDto> pending = adminCreatorService.getPendingCertificates();
        return ResponseEntity.ok(pending);
    }

    // Approve certificate
    @Operation(
        summary = "Approve a creator's certificates",
        description = """
            Accepts a pending creator application, moving the creator out of `PENDING` so they can publish
            courses. Recorded in the audit log against the acting admin's email.

            Only a creator still in `PENDING` can be approved; anything else is rejected.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Certificates approved.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "approved", value = """
                    { "message": "Certificate approved successfully" }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No creator exists with this id, the creator is not in `PENDING`, "
                + "`adminNote` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{creatorId}/approve-certificate")
    public ResponseEntity<Map<String, String>> approveCertificate(
            @Parameter(description = "Keycloak user id of the creator to approve.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId,
            @Parameter(description = "Reviewer's note, stored on the creator record.", required = true, example = "Chung chi JLPT N1 hop le")
            @RequestParam("adminNote")String adminNote,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");


        adminCreatorService.approveCertificate(creatorId, adminEmail, adminNote);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Certificate approved successfully");
        return ResponseEntity.ok(response);
    }

    // Reject certificate
    @Operation(
        summary = "Reject a creator's certificates",
        description = """
            Refuses a pending creator application. Recorded in the audit log against the acting admin's email.

            Only a creator still in `PENDING` can be rejected. The reason is read from the `reason` key of
            the request body; a body without it is accepted and the reason recorded as null.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Object carrying a `reason` key.",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "reject", value = """
                { "reason": "Chung chi khong doc duoc, vui long tai lai ban ro net" }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Certificates rejected.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "rejected", value = """
                    { "message": "Certificate rejected" }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No creator exists with this id, the creator is not in `PENDING`, "
                + "the body was malformed or missing, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{creatorId}/reject-certificate")
    public ResponseEntity<Map<String, String>> rejectCertificate(
            @Parameter(description = "Keycloak user id of the creator to reject.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId,
            @RequestBody Map<String, String> request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");
        String reason = request.get("reason");

        adminCreatorService.rejectCertificate(creatorId, reason, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Certificate rejected");
        return ResponseEntity.ok(response);
    }

    // Warn creator
    @Operation(
        summary = "Warn a creator",
        description = """
            Issues a formal warning, increments the creator's warning count and records the action in the
            audit log.

            **Three or more warnings within the last 90 days automatically moves the creator to
            `SUSPENDED`** — this endpoint can therefore suspend an account as a side effect. The response
            does not say whether that happened; re-read the creator detail to check.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warning issued. The creator may also have been auto-suspended.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "warned", value = """
                    { "message": "Warning issued successfully" }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No creator exists with this id, `reason` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{creatorId}/warn")
    public ResponseEntity<Map<String, String>> warnCreator(
            @Parameter(description = "Keycloak user id of the creator to warn.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId,
            @Parameter(description = "Why the warning is being issued. Stored on the warning and in the audit log.",
                    required = true, example = "Noi dung khoa hoc khong dung mo ta")
            @RequestParam("reason") String reason,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");


        adminCreatorService.warnCreator(creatorId, reason, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Warning issued successfully");
        return ResponseEntity.ok(response);
    }

    // Ban creator
    @Operation(
        summary = "Ban a creator",
        description = """
            Bans a creator and **bans every course they own** in the same operation. Their status becomes
            `BANNED` and the action is written to the audit log.

            Send `durationDays` for a temporary ban; omit it, or send zero or less, for a permanent one.
            Both values in the body are strings — `durationDays` is parsed as an integer, so a
            non-numeric value fails the request.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Object carrying `reason` and optionally `durationDays`, both as strings.",
        content = @Content(mediaType = "application/json",
            examples = {
                @ExampleObject(name = "temporary", value = """
                    { "reason": "Vi pham chinh sach noi dung nhieu lan", "durationDays": "30" }"""),
                @ExampleObject(name = "permanent", value = """
                    { "reason": "Lua dao hoc vien" }""")}))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Creator banned, and all their courses with them.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "banned", value = """
                    { "message": "Creator banned successfully" }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No creator exists with this id, `durationDays` was not a number, "
                + "the body was malformed or missing, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{creatorId}/ban")
    public ResponseEntity<Map<String, String>> banCreator(
            @Parameter(description = "Keycloak user id of the creator to ban.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId,
            @RequestBody Map<String, String> request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");
        String reason = request.get("reason");
        Integer durationDays = request.get("durationDays") != null ? Integer.parseInt(request.get("durationDays"))
                : null;

        adminCreatorService.banCreator(creatorId, reason, durationDays, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Creator banned successfully");
        return ResponseEntity.ok(response);
    }

    // Unban creator
    @Operation(
        summary = "Unban a creator",
        description = """
            Lifts a ban, clears any expiry date, sets the creator's status to `SUCCESS` and **unbans every
            course they own**.

            Note this restores the creator to `SUCCESS` regardless of what their status was before the
            ban — a creator who was `PENDING` or `SUSPENDED` when banned comes back approved.

            The body is optional; when omitted the audit reason is recorded as "Unbanned by admin".
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = false,
        description = "Optional object carrying a `reason` key.",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "unban", value = """
                { "reason": "Khieu nai duoc chap nhan sau khi xem xet lai" }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Creator unbanned, and all their courses with them.",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "unbanned", value = """
                    { "message": "Creator unbanned successfully" }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "No creator exists with this id, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{creatorId}/unban")
    public ResponseEntity<Map<String, String>> unbanCreator(
            @Parameter(description = "Keycloak user id of the creator to unban.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId,
            @RequestBody(required = false) Map<String, String> request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");
        String reason = request != null ? request.get("reason") : "Unbanned by admin";

        adminCreatorService.unbanCreator(creatorId, reason, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Creator unbanned successfully");
        return ResponseEntity.ok(response);
    }

    // Get violation history
    @Operation(
        summary = "Get a creator's violation history",
        description = "Returns every report learners have filed against courses owned by this creator — the evidence base for a warning or ban.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports against this creator's courses. Empty when there are none. "
                + "Also empty, rather than an error, for a creator id that does not exist.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = Report.class)))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{creatorId}/violations")
    public ResponseEntity<List<Report>> getCreatorViolations(
            @Parameter(description = "Keycloak user id of the creator.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId) {
        List<Report> violations = adminCreatorService.getCreatorViolationHistory(creatorId);
        return ResponseEntity.ok(violations);
    }

    // Get audit logs
    @Operation(
        summary = "Get the audit trail for a creator",
        description = """
            Returns every administrative action taken against this creator — approvals, rejections,
            warnings, bans and unbans — each with the acting admin's email and the stated reason.

            This is the same trail the creator sees at `GET /api/creator/auditlog`, but for any creator.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit entries for this creator. Empty when no action has been taken. "
                + "Also empty, rather than an error, for a creator id that does not exist.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = AuditLog.class)))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "403", description = "The token does not carry the `ADMIN` role.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{creatorId}/audit-logs")
    public ResponseEntity<List<AuditLog>> getCreatorAuditLogs(
            @Parameter(description = "Keycloak user id of the creator.", required = true, example = "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f")
            @PathVariable String creatorId) {
        List<AuditLog> logs = adminCreatorService.getCreatorAuditLog(creatorId);
        return ResponseEntity.ok(logs);
    }
}
