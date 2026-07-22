package com.jpd.web.controller.creator;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.AuditLog;
import com.jpd.web.service.AuditLogService;

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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creator/auditlog")
@Tag(name = "Creator audit log", description = "Administrative actions taken against the signed-in creator's account.")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class CreatorAuditLogController {
    @Autowired
    private AuditLogService auditLogService;

    @Operation(
        summary = "List admin actions taken on the creator's account",
        description = """
            Returns the audit trail for the signed-in creator: certificate approvals and rejections, bans,
            warnings and unbans, each with the acting admin's email and the stated reason.

            Read-only, and scoped to the token's `sub` claim — a creator can only see entries about
            themselves. Note that the acting admin's email address is exposed to the creator.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit entries for this creator. Empty when no admin action has been taken.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = AuditLog.class)),
                examples = @ExampleObject(name = "auditLog", value = """
                    [
                      {
                        "auditLogId": 15,
                        "actionType": "APPROVE_CERT",
                        "targetCreatorId": "7f3c1a90-2b45-4e88-9f01-0a1b2c3d4e5f",
                        "adminEmail": "admin@jaen.vn",
                        "reason": "Chung chi JLPT N1 hop le",
                        "timestamp": "2026-07-20T10:04:11.000"
                      }
                    ]"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping()
    public ResponseEntity<?> getAuditLog(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                         @Parameter(hidden = true) HttpServletRequest request){
      String creatorId =jwt.getClaimAsString("sub");
        List <AuditLog> as=this.auditLogService.getLogsByCreator(creatorId);
        return ResponseEntity.ok(as);
    }
}
