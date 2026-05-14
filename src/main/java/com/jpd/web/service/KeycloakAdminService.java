package com.jpd.web.service;

import jakarta.annotation.PostConstruct;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class KeycloakAdminService {
    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    private Keycloak keycloak;

    @PostConstruct
    public void initKeycloak() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakServerUrl)
                .realm(realm) // jpdweb
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId) // backend-service
                .clientSecret(clientSecret)
                .build();
    }

    public void assignClientRoleToUser(String userId) {
    String roleName="creator";
        try {
            // Làm việc với realm "jpdweb" (realm chứa users và clients)
            RealmResource realmResource = keycloak.realm(realm); // realm = jpdweb

            // Tìm client
            ClientRepresentation client = realmResource.clients()
                    .findByClientId(clientId)
                    .get(0);

            String clientUuid = client.getId();

            // Lấy role
            RoleRepresentation role = realmResource.clients()
                    .get(clientUuid)
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            // Gán role
            realmResource.users()
                    .get(userId)
                    .roles()
                    .clientLevel(clientUuid)
                    .add(Arrays.asList(role));



        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to assign role", e);
        }
    }
}