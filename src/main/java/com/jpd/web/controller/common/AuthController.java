package com.jpd.web.controller.common;
//1. AuthController.java - PKCE VERSION

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = """
        Keycloak OAuth2 Authorization Code flow with PKCE, brokered by the backend.

        The browser never handles tokens directly: after the code exchange the backend writes the
        access and refresh tokens into `HttpOnly` cookies (`access_token`, `refresh_token`, `SameSite=Lax`,
        path `/`). Every endpoint here is public — no bearer token is required to call them.
        """)
public class AuthController {

 @Value("${keycloak.auth-uri}")
 private String keycloakAuthUri;

 @Value("${keycloak.token-uri}")
 private String keycloakTokenUri;

 @Value("${keycloak.logout-uri}")
 private String keycloakLogoutUri;

 @Value("${keycloak.client-id}")
 private String clientId;

 @Value("${app.frontend-url}")
 private String frontendUrl;

 private final RestTemplate restTemplate = new RestTemplate();
 private final Map<String, String> codeVerifierStore = new HashMap<>();

 @Operation(
     summary = "Start login: build the Keycloak authorization URL",
     description = """
         First step of the PKCE flow. Generates a random `code_verifier`, derives its SHA-256
         `code_challenge`, generates an opaque `state`, and returns the Keycloak authorization URL the
         browser should be sent to (`response_type=code`, `code_challenge_method=S256`,
         `scope=openid profile email`).

         The `code_verifier` is kept server-side keyed by `state` and consumed by `POST /api/auth/callback`.
         The caller must pass the returned `state` back unchanged.

         Public endpoint — no token required.
         """)
 @ApiResponses({
     @ApiResponse(responseCode = "200", description = "Authorization URL and the `state` to echo back on callback.",
         content = @Content(mediaType = "application/json",
             examples = @ExampleObject(name = "loginUrl", value = """
                 {
                   "loginUrl": "https://keycloak.example.com/realms/jaen/protocol/openid-connect/auth?client_id=backended-service&response_type=code&redirect_uri=http://localhost:3000/callback&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256&state=Zm9vYmFyYmF6cXV4MTIzNA&scope=openid profile email",
                   "state": "Zm9vYmFyYmF6cXV4MTIzNA"
                 }"""))),
     @ApiResponse(responseCode = "500", description = "The PKCE challenge could not be generated.",
         content = @Content(mediaType = "application/json",
             examples = @ExampleObject(name = "failure", value = """
                 { "error": "Failed to generate login URL" }""")))
 })
 @GetMapping("/login-url")
 public ResponseEntity<?> getLoginUrl(
         @Parameter(description = "Where Keycloak should send the browser after login. Must exactly match a redirect URI "
                 + "registered on the Keycloak client, and must be sent again on `POST /api/auth/callback`.",
                 required = true, example = "http://localhost:3000/callback")
         @RequestParam String redirectUri) {
     try {

         String codeVerifier = generateCodeVerifier();
         String codeChallenge = generateCodeChallenge(codeVerifier);
         String state = generateState();

         codeVerifierStore.put(state, codeVerifier);

         String loginUrl = keycloakAuthUri +
                 "?client_id=" + clientId +
                 "&response_type=code" +
                 "&redirect_uri=" + redirectUri +
                 "&code_challenge=" + codeChallenge +
                 "&code_challenge_method=S256" +
                 "&state=" + state +
                 "&scope=openid profile email";

         return ResponseEntity.ok(Map.of(
                 "loginUrl", loginUrl,
                 "state", state
         ));
     } catch (Exception e) {

         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(Map.of("error", "Failed to generate login URL"));
         
     }
 }

 @Operation(
     summary = "Complete login: exchange the authorization code for tokens",
     description = """
         Second step of the PKCE flow. Send the `code` and `state` Keycloak returned to the redirect URI,
         plus the same `redirectUri` used in `GET /api/auth/login-url`.

         The backend looks up the stored `code_verifier` by `state` (consuming it — a `state` works only
         once), exchanges the code at Keycloak's token endpoint, and writes `access_token` and
         `refresh_token` into `HttpOnly` cookies. The tokens themselves are never in the response body.

         Public endpoint — no token required.
         """)
 @io.swagger.v3.oas.annotations.parameters.RequestBody(
     required = true,
     description = "Values returned by Keycloak on the redirect, plus the redirect URI used to start the flow.",
     content = @Content(mediaType = "application/json",
         examples = @ExampleObject(name = "callback", value = """
             {
               "code": "8f1e2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b.1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d.backended-service",
               "state": "Zm9vYmFyYmF6cXV4MTIzNA",
               "redirectUri": "http://localhost:3000/callback"
             }""")))
 @ApiResponses({
     @ApiResponse(responseCode = "200", description = "Tokens obtained and written to `HttpOnly` cookies. Two `Set-Cookie` headers are returned.",
         content = @Content(mediaType = "application/json",
             examples = @ExampleObject(name = "success", value = """
                 { "success": true, "expiresIn": 300 }"""))),
     @ApiResponse(responseCode = "400", description = "`code` or `state` missing from the body, or `state` unknown/already consumed.",
         content = @Content(mediaType = "application/json",
             examples = {
                 @ExampleObject(name = "missing", value = """
                     { "error": "Missing code or state" }"""),
                 @ExampleObject(name = "badState", value = """
                     { "error": "Invalid state" }""")})),
     @ApiResponse(responseCode = "401", description = "Keycloak rejected the code exchange (expired or already-used code, redirect URI mismatch, PKCE verifier mismatch).",
         content = @Content(mediaType = "application/json",
             examples = @ExampleObject(name = "exchangeFailed", value = """
                 { "error": "Token exchange failed: 400 Bad Request from Keycloak (invalid_grant)" }""")))
 })
 @PostMapping("/callback")
 public ResponseEntity<?> handleCallback(@RequestBody Map<String, String> callbackData,
                                         HttpServletResponse response) {
     String code = callbackData.get("code");
     String state = callbackData.get("state");
     String redirectUri = callbackData.get("redirectUri");

     if (code == null || state == null) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                 .body(Map.of("error", "Missing code or state"));
     }

     String codeVerifier = codeVerifierStore.remove(state);
     if (codeVerifier == null) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                 .body(Map.of("error", "Invalid state"));
     }

     MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
     formData.add("grant_type", "authorization_code");
     formData.add("client_id", clientId);
     formData.add("code", code);
     formData.add("redirect_uri", redirectUri);
     formData.add("code_verifier", codeVerifier);

     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

     HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

     try {
         ResponseEntity<Map> keycloakResponse = restTemplate.postForEntity(keycloakTokenUri, request, Map.class);
         Map<String, Object> tokens = keycloakResponse.getBody();

         setTokenCookies(response,
                 (String) tokens.get("access_token"),
                 (String) tokens.get("refresh_token"),
                 (Integer) tokens.get("expires_in"),
                 (Integer) tokens.get("refresh_expires_in"));

         return ResponseEntity.ok(Map.of(
                 "success", true,
                 "expiresIn", tokens.get("expires_in")
         ));
     } catch (Exception e) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(Map.of("error", "Token exchange failed: " + e.getMessage()));
     }
 }

 @Operation(
     summary = "Refresh the access token",
     description = """
         Exchanges the `refresh_token` cookie for a fresh token pair at Keycloak and rewrites both
         `HttpOnly` cookies. Nothing is read from the request body — the refresh token is taken from the
         cookie only.

         If Keycloak rejects the refresh token, both cookies are cleared before the 401 is returned, so
         the client ends up in a clean signed-out state.

         Public endpoint — no bearer token required, but the `refresh_token` cookie must be present.
         """)
 @ApiResponses({
     @ApiResponse(responseCode = "200", description = "New tokens issued and cookies rewritten.",
         content = @Content(mediaType = "application/json",
             examples = @ExampleObject(name = "refreshed", value = """
                 { "success": true }"""))),
     @ApiResponse(responseCode = "401", description = "No `refresh_token` cookie, or Keycloak rejected it (expired, revoked, or session ended). Cookies are cleared in the latter case.",
         content = @Content(mediaType = "application/json",
             examples = {
                 @ExampleObject(name = "noCookie", value = """
                     { "error": "No refresh token" }"""),
                 @ExampleObject(name = "rejected", value = """
                     { "error": "Invalid refresh token" }""")}))
 })
 @PostMapping("/refresh")
 public ResponseEntity<?> refreshToken(
                                       @Parameter(in = ParameterIn.COOKIE, name = "refresh_token",
                                               description = "HttpOnly refresh-token cookie set by `/api/auth/callback`. Sent automatically by the browser.")
                                       @CookieValue(name = "refresh_token", required = false) String refreshToken,
                                       HttpServletResponse response) {
     if (refreshToken == null) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(Map.of("error", "No refresh token"));
     }

     MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
     formData.add("grant_type", "refresh_token");
     formData.add("client_id", clientId);
     formData.add("refresh_token", refreshToken);

     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

     HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

     try {
         ResponseEntity<Map> keycloakResponse = restTemplate.postForEntity(keycloakTokenUri, request, Map.class);
         Map<String, Object> tokens = keycloakResponse.getBody();

         setTokenCookies(response,
                 (String) tokens.get("access_token"),
                 (String) tokens.get("refresh_token"),
                 (Integer) tokens.get("expires_in"),
                 (Integer) tokens.get("refresh_expires_in"));

         return ResponseEntity.ok(Map.of("success", true));
     } catch (Exception e) {
         clearTokenCookies(response);
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(Map.of("error", "Invalid refresh token"));
     }
 }

 @Operation(
     summary = "Log out",
     description = """
         Ends the Keycloak session for the `refresh_token` cookie and clears both auth cookies by
         setting them to an empty value with `Max-Age=0`.

         Always succeeds. If the cookie is absent, or the call to Keycloak's logout endpoint fails, the
         cookies are still cleared and 200 is returned — the client is signed out locally either way.

         Public endpoint — no bearer token required.
         """)
 @ApiResponse(responseCode = "200", description = "Cookies cleared. Two expiring `Set-Cookie` headers are returned.",
     content = @Content(mediaType = "application/json",
         examples = @ExampleObject(name = "loggedOut", value = """
             { "success": true }""")))
 @PostMapping("/logout")
 public ResponseEntity<?> logout(
                                 @Parameter(in = ParameterIn.COOKIE, name = "refresh_token",
                                         description = "HttpOnly refresh-token cookie. Optional — logout succeeds without it.")
                                 @CookieValue(name = "refresh_token", required = false) String refreshToken,
                                 HttpServletResponse response) {
     if (refreshToken != null) {
         try {
             MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
             formData.add("client_id", clientId);
             formData.add("refresh_token", refreshToken);

             HttpHeaders headers = new HttpHeaders();
             headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

             HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
             restTemplate.postForEntity(keycloakLogoutUri, request, String.class);
         } catch (Exception e) {
             System.err.println("Keycloak logout failed: " + e.getMessage());
         }
     }

     clearTokenCookies(response);
     return ResponseEntity.ok(Map.of("success", true));
 }

 private String generateCodeVerifier() {
     SecureRandom secureRandom = new SecureRandom();
     byte[] codeVerifier = new byte[32];
     secureRandom.nextBytes(codeVerifier);
     return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
 }

 private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
     byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
     MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
     messageDigest.update(bytes, 0, bytes.length);
     byte[] digest = messageDigest.digest();
     return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
 }

 private String generateState() {
     SecureRandom secureRandom = new SecureRandom();
     byte[] state = new byte[16];
     secureRandom.nextBytes(state);
     return Base64.getUrlEncoder().withoutPadding().encodeToString(state);
 }

 private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken,
                               Integer accessExpiry, Integer refreshExpiry) {
     Cookie accessCookie = new Cookie("access_token", accessToken);
     accessCookie.setHttpOnly(true);
     accessCookie.setSecure(false);
     accessCookie.setPath("/");
     accessCookie.setMaxAge(accessExpiry != null ? accessExpiry : 300);
     accessCookie.setAttribute("SameSite", "Lax");
     response.addCookie(accessCookie);

     if (refreshToken != null) {
         Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
         refreshCookie.setHttpOnly(true);
         refreshCookie.setSecure(false);
         refreshCookie.setPath("/");
         refreshCookie.setMaxAge(refreshExpiry != null ? refreshExpiry : 1800);
         refreshCookie.setAttribute("SameSite", "Lax");
         response.addCookie(refreshCookie);
     }
 }

 private void clearTokenCookies(HttpServletResponse response) {
     Cookie accessCookie = new Cookie("access_token", "");
     accessCookie.setHttpOnly(true);
     accessCookie.setSecure(false);
     accessCookie.setPath("/");
     accessCookie.setMaxAge(0);
     response.addCookie(accessCookie);

     Cookie refreshCookie = new Cookie("refresh_token", "");
     refreshCookie.setHttpOnly(true);
     refreshCookie.setSecure(false);
     refreshCookie.setPath("/");
     refreshCookie.setMaxAge(0);
     response.addCookie(refreshCookie);
 }

}