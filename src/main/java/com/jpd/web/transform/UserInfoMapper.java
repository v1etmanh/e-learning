package com.jpd.web.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

import com.jpd.web.dto.UserInfoDto;

public final class UserInfoMapper {

    private static final String CLIENT_ID = "backended-service";

    private UserInfoMapper() {
    }

    public static UserInfoDto fromJwt(Jwt jwt) {

        List<String> roles = extractAllRoles(jwt);

        boolean isAdmin = roles.stream()
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN"));

        boolean isCreator = roles.stream()
                .anyMatch(r -> r.equalsIgnoreCase("CREATOR"));

        return UserInfoDto.builder()
                .userName(jwt.getClaimAsString("preferred_username"))
                .givenName(jwt.getClaimAsString("given_name"))
                .familyName(jwt.getClaimAsString("family_name"))
                .email(jwt.getClaimAsString("email"))
                .Role(String.join(",", roles))
                .isAdmin(isAdmin)
                .isCreator(isCreator)
                .createDate(null)
                .build();
    }

    /**
     * Extract roles from both realm_access and resource_access
     */
    private static List<String> extractAllRoles(Jwt jwt) {
        List<String> allRoles = new ArrayList<>();

        // Extract realm roles
        List<String> realmRoles = extractRealmRoles(jwt);
        allRoles.addAll(realmRoles);

        // Extract client/resource roles
        List<String> clientRoles = extractClientRoles(jwt);
        allRoles.addAll(clientRoles);

        // Remove duplicates and filter out system roles
        return allRoles.stream()
                .distinct()
                .filter(role -> !isSystemRole(role))
                .collect(Collectors.toList());
    }

    /**
     * Extract roles from realm_access
     */
    @SuppressWarnings("unchecked")
    private static List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");

        return roles instanceof List
                ? (List<String>) roles
                : List.of();
    }

    /**
     * Extract roles from resource_access for specific client
     */
    @SuppressWarnings("unchecked")
    private static List<String> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null) {
            return List.of();
        }

        Map<String, Object> client = (Map<String, Object>) resourceAccess.get(CLIENT_ID);

        if (client == null) {
            return List.of();
        }

        Object roles = client.get("roles");

        return roles instanceof List
                ? (List<String>) roles
                : List.of();
    }

    /**
     * Filter out Keycloak system roles
     */
    private static boolean isSystemRole(String role) {
        return role.equalsIgnoreCase("offline_access")
                || role.equalsIgnoreCase("uma_authorization")
                || role.startsWith("default-roles-")
                || role.equalsIgnoreCase("manage-account")
                || role.equalsIgnoreCase("manage-account-links")
                || role.equalsIgnoreCase("view-profile");
    }
}