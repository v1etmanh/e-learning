package com.jpd.web.testutil;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds fake JWTs for MockMvc requests, mirroring the ROLE_&lt;X&gt; authority shape
 * that {@code com.jpd.web.config.JwtRoleConverted} derives from a real Keycloak token.
 * The {@code jwt()} post-processor never invokes the app's real JwtDecoder/JWKS.
 */
public final class JwtTestDataBuilder {

    private JwtTestDataBuilder() {
    }

    public static JwtRequestPostProcessor jwtWithRoles(String subject, String... roles) {
        List<String> authorities = Arrays.stream(roles)
                .map(role -> "ROLE_" + role.toUpperCase())
                .collect(Collectors.toList());

        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder
                        .subject(subject)
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .claim("realm_access", java.util.Map.of("roles", Arrays.asList(roles))))
                .authorities(authorities.stream()
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
    }

    public static JwtRequestPostProcessor jwtForAdmin(String subject) {
        return jwtWithRoles(subject, "ADMIN");
    }

    public static JwtRequestPostProcessor jwtForCreator(String subject) {
        return jwtWithRoles(subject, "CREATOR");
    }

    public static JwtRequestPostProcessor jwtForCustomer(String subject) {
        return jwtWithRoles(subject, "CUSTOMER");
    }

    public static Jwt.Builder rawJwt(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }
}
