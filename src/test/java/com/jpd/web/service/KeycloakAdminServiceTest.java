package com.jpd.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminServiceTest {

    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realmResource;
    @Mock
    private ClientsResource clientsResource;
    @Mock
    private ClientResource clientResource;
    @Mock
    private RolesResource rolesResource;
    @Mock
    private RoleResource roleResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private RoleMappingResource roleMappingResource;
    @Mock
    private RoleScopeResource roleScopeResource;

    private KeycloakAdminService keycloakAdminService;

    @BeforeEach
    void setUp() {
        keycloakAdminService = new KeycloakAdminService();
        ReflectionTestUtils.setField(keycloakAdminService, "realm", "jpdweb");
        ReflectionTestUtils.setField(keycloakAdminService, "clientId", "backended-service");
        ReflectionTestUtils.setField(keycloakAdminService, "keycloak", keycloak);
        // @PostConstruct initKeycloak() never runs outside a Spring context - the mock
        // Keycloak client above is injected directly, so no real KeycloakBuilder call happens.
    }

    @Test
    void assignClientRoleToUser_happyPath_addsRoleAtClientLevel() {
        when(keycloak.realm("jpdweb")).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        ClientRepresentation client = new ClientRepresentation();
        client.setId("client-uuid");
        when(clientsResource.findByClientId("backended-service")).thenReturn(List.of(client));
        when(clientsResource.get("client-uuid")).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("creator")).thenReturn(roleResource);
        RoleRepresentation role = new RoleRepresentation();
        role.setName("creator");
        when(roleResource.toRepresentation()).thenReturn(role);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-1")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel("client-uuid")).thenReturn(roleScopeResource);

        keycloakAdminService.assignClientRoleToUser("user-1");

        verify(roleScopeResource).add(List.of(role));
    }

    @Test
    void wrapsAnyExceptionInTheChain_asRuntimeException() {
        when(keycloak.realm("jpdweb")).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        // No client found for this clientId -> findByClientId returns empty list -> .get(0) throws
        when(clientsResource.findByClientId("backended-service")).thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> keycloakAdminService.assignClientRoleToUser("user-1"));
    }
}
