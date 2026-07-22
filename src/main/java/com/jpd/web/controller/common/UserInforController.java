package com.jpd.web.controller.common;

import com.jpd.web.transform.UserInfoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/customer")
@Tag(name = "Account", description = "Identity of the caller, resolved from the bearer token.")
public class UserInforController {

    @Operation(
        summary = "Get the caller's account information",
        description = """
            Returns the signed-in user's profile and role flags, built entirely from the claims of the
            supplied JWT — `preferred_username`, `given_name`, `family_name`, `email`, `realm_access.roles`
            and `resource_access.backended-service.roles`. No database lookup is performed and
            `createDate` is always `null`.

            This path is registered as `permitAll` in the security configuration, so the resource server
            does not reject an anonymous request. The controller then dereferences the token and fails,
            which surfaces as 500 — see the 500 response below. A bearer token is required in practice.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account information for the bearer token.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserInfoDto.class),
                examples = @ExampleObject(name = "creator", value = """
                    {
                      "userName": "thuynguyen",
                      "familyName": "Nguyễn",
                      "role": "USER,CREATOR",
                      "givenName": "Thuỷ",
                      "createDate": null,
                      "email": "nguyenthuy010605@gmail.com",
                      "creator": true,
                      "admin": false
                    }"""))),
        // Known gap: the path is permitAll, so an anonymous caller reaches the controller with a null
        // Jwt and the mapper throws NullPointerException, handled as a generic 500 rather than 401.
        @ApiResponse(responseCode = "500", description = "Called without a bearer token, or any other unexpected server error.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
    @GetMapping("/account_infor")
    public ResponseEntity<UserInfoDto> getAccountInfor(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UserInfoDto userInfo = UserInfoMapper.fromJwt(jwt);
        return ResponseEntity.ok(userInfo);
    }
}
