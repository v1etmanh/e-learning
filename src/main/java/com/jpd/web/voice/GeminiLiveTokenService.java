package com.jpd.web.voice;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeminiLiveTokenService {
    private final RestTemplate restTemplate;

    // The key must remain server-side; allow deployment configuration through
    // GEMINI_API_KEY while preserving the existing application property.
    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.live.model:gemini-3.1-flash-live-preview}")
    private String model;

    @Value("${gemini.live.token-url:https://generativelanguage.googleapis.com/v1alpha/auth_tokens}")
    private String tokenUrl;

    public GeminiLiveTokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public VoiceLiveTokenResponse createToken(VoiceCharacterProfile profile) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini Live is not configured");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(30, ChronoUnit.MINUTES);
        Instant newSessionExpiresAt = now.plus(1, ChronoUnit.MINUTES);

        Map<String, Object> setup = new LinkedHashMap<>();
        setup.put("model", "models/" + model);
        setup.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", profile.systemInstruction()
                        + " Keep every reply short: no more than two brief sentences, about 10 seconds of speech, and finish with one simple question."
                        + " Finish the current sentence before listening; do not stop mid-sentence."))
        ));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.8);
        generationConfig.put("maxOutputTokens", 256);
        generationConfig.put("responseModalities", List.of("AUDIO"));
        generationConfig.put("speechConfig", Map.of(
                "voiceConfig", Map.of(
                        "prebuiltVoiceConfig", Map.of("voiceName", profile.liveVoice())
                )
        ));
        setup.put("generationConfig", generationConfig);
        setup.put("inputAudioTranscription", Map.of());
        setup.put("outputAudioTranscription", Map.of());
        setup.put("sessionResumption", Map.of());
        Map<String, Object> automaticActivityDetection = new LinkedHashMap<>();
        automaticActivityDetection.put("disabled", false);
        automaticActivityDetection.put("startOfSpeechSensitivity", "START_SENSITIVITY_HIGH");
        automaticActivityDetection.put("endOfSpeechSensitivity", "END_SENSITIVITY_LOW");
        automaticActivityDetection.put("prefixPaddingMs", 200);
        automaticActivityDetection.put("silenceDurationMs", 600);
        setup.put("realtimeInputConfig", Map.of(
                "automaticActivityDetection", automaticActivityDetection,
                "activityHandling", "NO_INTERRUPTION"
        ));

        Map<String, Object> authToken = new LinkedHashMap<>();
        authToken.put("uses", 1);
        authToken.put("expireTime", expiresAt.toString());
        authToken.put("newSessionExpireTime", newSessionExpiresAt.toString());
        authToken.put("bidiGenerateContentSetup", setup);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // The REST endpoint maps the request body directly to AuthToken.
        // Wrapping it as {"authToken": {...}} causes INVALID_ARGUMENT:
        // Unknown name "authToken" at 'auth_token'.
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(authToken, headers);

        String url = UriComponentsBuilder.fromUriString(tokenUrl)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            Object accessToken = response.getBody() == null ? null : response.getBody().get("name");
            if (!(accessToken instanceof String token) || token.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini did not return an ephemeral token");
            }
            return new VoiceLiveTokenResponse(token, model, profile.id(), expiresAt.toString());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create Gemini Live session", exception);
        }
    }
}
