package com.jpd.web.dto;

import java.sql.Date;

import lombok.Builder;
import org.springframework.web.bind.annotation.RequestMapping;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
@Schema(name = "UserInfoDto", description = "Account summary derived entirely from the caller's JWT claims. Nothing is read from the database.")
public class UserInfoDto {

@Schema(description = "Keycloak `preferred_username` claim.", example = "thuynguyen")
private String userName;

@Schema(description = "Keycloak `family_name` claim (surname).", example = "Nguyễn")
private String familyName;

@Schema(description = "Comma-separated realm and client roles from the token, with Keycloak system roles "
		+ "(`offline_access`, `uma_authorization`, `default-roles-*`, `manage-account`, `manage-account-links`, `view-profile`) filtered out.",
		example = "USER,CREATOR")
private String Role;

@Schema(description = "Keycloak `given_name` claim (first name).", example = "Thuỷ")
private String givenName;

@Schema(description = "Always `null` — the mapper does not populate this field.", example = "null")
private Date createDate;

@Schema(description = "Keycloak `email` claim.", example = "nguyenthuy010605@gmail.com")
private String email;

@Schema(description = "True when the token carries the `CREATOR` role.", example = "false")
private boolean isCreator;

@Schema(description = "True when the token carries the `ADMIN` role.", example = "false")
private boolean isAdmin;


}
