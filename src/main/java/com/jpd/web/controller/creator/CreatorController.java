package com.jpd.web.controller.creator;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.CreatorDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.CreatorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@RestController
@Slf4j
@RequestMapping("api/creator/")
@Tag(name = "Creator profile", description = """
        The signed-in creator's own profile and qualification certificates.

        Note: `/api/creator/**` is not role-restricted in the security configuration — any authenticated
        user reaches these handlers. Authorization comes from the creator record being looked up by the
        token's `sub` claim, so a non-creator gets a 404 rather than a 403.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class CreatorController {
@Autowired
private CreatorService creatorService;

@Operation(
    summary = "Get the signed-in creator's profile",
    description = "Returns the creator record belonging to the `sub` claim of the bearer token, including review status and certificate URLs.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "The creator's profile.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreatorDto.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/getAccount")
public ResponseEntity<CreatorDto> getAccount(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    String creatorId = jwt.getClaimAsString("sub");
    CreatorDto c=this.creatorService.getAccount(creatorId);
    return ResponseEntity.status(HttpStatus.OK).body(c);
}
@Operation(
    summary = "Upload qualification certificates",
    description = """
        Uploads one or more certificate files to Firebase Storage and appends their URLs to the signed-in
        creator's profile. Send them as repeated `certificateFile` parts.

        Empty files in the batch are silently skipped. Existing certificates are kept — this appends
        rather than replaces. The response carries no body; re-read the profile to see the new URLs.

        Note the path is spelled `upade_certificate`, not `update_certificate`.
        """)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Certificates uploaded and appended to the profile. Empty body.", content = @Content),
    @ApiResponse(responseCode = "400", description = "A file could not be uploaded to Firebase Storage (`code: FILE_UPLOAD_ERROR`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
    @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "No `certificateFile` part was sent, or another unexpected server error occurred.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
@PostMapping("/upade_certificate") // Nên sửa thành /update_certificate
public ResponseEntity<?> updateCertificate(
        @Parameter(description = "One or more certificate files. Repeat the part to send several. Empty files are skipped.",
                required = true, array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                        schema = @Schema(type = "string", format = "binary")))
        @RequestParam("certificateFile") MultipartFile[] files, // Đổi thành array
        @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) throws FileUploadException {


    String creatorId = jwt.getClaimAsString("sub");
    log.info("Uploading {} certificates for creator {}", files.length, creatorId);
    
    // Validate và upload từng file
    for (MultipartFile file : files) {
        if (file.isEmpty()) {
            continue;
        }
        this.creatorService.upLoadCertificate(creatorId, file);
    }
    
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
    @Operation(
        summary = "Remove a qualification certificate",
        description = """
            Detaches a certificate URL from the signed-in creator's profile. The URL must currently be on
            the caller's own profile — removing someone else's is rejected with 401.

            The response body is the plain-text string `Certificate removed successfully`, not JSON.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Certificate removed. Plain-text body.",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Certificate removed successfully"))),
        @ApiResponse(responseCode = "401", description = "The URL is not on the caller's profile, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "The caller has no creator profile (`code: CREATOR_NOT_FOUND`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "`certificateUrl` was omitted, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/removeCertificate")
    public ResponseEntity<?> removeCertificate(
           @Parameter(description = "The exact certificate URL to detach, as returned on the creator profile.", required = true,
                   example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/certificate%2Fjlpt-n1.pdf?alt=media")
           @RequestParam("certificateUrl")String certificateUrl,  // Nhận từ body
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        String creatorId = jwt.getClaimAsString("sub");


        log.info("Removing certificate for creator {}: {}", creatorId, certificateUrl);

        this.creatorService.removeCertificate(creatorId, certificateUrl);

        return ResponseEntity.status(HttpStatus.OK).body("Certificate removed successfully");
    }
}
