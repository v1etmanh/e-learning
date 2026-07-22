package com.jpd.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenAPIConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI jaenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jaen E-Learning API")
                        .version("v1")
                        .description("""
                                REST API for the Jaen e-learning platform.

                                Authentication is handled by Keycloak. Protected endpoints expect an
                                `Authorization: Bearer <access token>` header containing an OAuth2 JWT.
                                The user identity is read from the token claims (`sub`, `email`) — it is
                                never taken from the request body or query string.
                                """)
                        .contact(new Contact().name("Jaen Team")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Keycloak-issued OAuth2 access token.")));
    }
}
