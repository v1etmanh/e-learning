package com.jpd.web.voice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiLiveTokenServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiLiveTokenService service;

    private final VoiceCharacterProfile profile = new VoiceCharacterProfile(
            "dog", "Pip the Dog", "desc", "Companion", "Puck", "You are Pip.");

    @BeforeEach
    void setUp() {
        service = new GeminiLiveTokenService(restTemplate);
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "tokenUrl", "https://gemini.example.com/token");
    }

    @Test
    void throwsServiceUnavailable_whenApiKeyBlank_beforeAnyHttpCall() {
        ReflectionTestUtils.setField(service, "apiKey", "");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createToken(profile));

        assertThat(ex.getStatusCode().value()).isEqualTo(503);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void throwsBadGateway_whenResponseMissingToken() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("someOtherField", "x")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createToken(profile));

        assertThat(ex.getStatusCode().value()).isEqualTo(502);
    }

    @Test
    void wrapsOtherExceptions_asBadGateway_notDoubleWrapped() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(new RestClientException("network down"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createToken(profile));

        assertThat(ex.getStatusCode().value()).isEqualTo(502);
        assertThat(ex.getReason()).isEqualTo("Unable to create Gemini Live session");
    }

    @Test
    void happyPath_returnsTokenBuiltFromResponse_andBuildsCorrectSetupPayload() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("name", "ephemeral-token-123")));

        VoiceLiveTokenResponse result = service.createToken(profile);

        assertThat(result.accessToken()).isEqualTo("ephemeral-token-123");
        assertThat(result.model()).isEqualTo("test-model");
        assertThat(result.characterId()).isEqualTo("dog");
    }
}
