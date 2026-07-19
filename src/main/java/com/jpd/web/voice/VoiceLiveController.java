package com.jpd.web.voice;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/voice")
public class VoiceLiveController {
    private final VoiceCharacterCatalog catalog;
    private final GeminiLiveTokenService tokenService;
    private final VoiceConversationProgressService progressService;

    public VoiceLiveController(VoiceCharacterCatalog catalog, GeminiLiveTokenService tokenService,
            VoiceConversationProgressService progressService) {
        this.catalog = catalog;
        this.tokenService = tokenService;
        this.progressService = progressService;
    }

    @GetMapping("/characters")
    public ResponseEntity<List<VoiceCharacterSummary>> characters() {
        return ResponseEntity.ok(catalog.all().stream().map(VoiceCharacterSummary::from).toList());
    }

    @GetMapping("/characters/{characterId}")
    public ResponseEntity<VoiceCharacterSummary> character(@PathVariable String characterId) {
        VoiceCharacterProfile profile = catalog.get(characterId);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voice character not found");
        }
        return ResponseEntity.ok(VoiceCharacterSummary.from(profile));
    }

    @PostMapping("/live/token")
    public ResponseEntity<VoiceLiveTokenResponse> createLiveToken(
            @Valid @RequestBody VoiceLiveStartRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        VoiceCharacterProfile profile = catalog.get(request.characterId());
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voice character not found");
        }
        return ResponseEntity.ok(tokenService.createToken(profile));
    }

    @PostMapping("/progress/sessions")
    public ResponseEntity<VoiceConversationSessionResponse> startConversationTracking(
            @Valid @RequestBody VoiceConversationStartRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = requireUserId(jwt);
        if (catalog.get(request.characterId()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voice character not found");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VoiceConversationSessionResponse.from(progressService.start(userId, request.characterId())));
    }

    @PostMapping("/progress/sessions/{sessionId}/complete")
    public ResponseEntity<VoiceConversationSessionResponse> completeConversationTracking(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = requireUserId(jwt);
        try {
            return ResponseEntity.ok(VoiceConversationSessionResponse.from(
                    progressService.complete(userId, sessionId)));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    @GetMapping("/progress/daily")
    public ResponseEntity<VoiceDailyProgressResponse> dailyProgress(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(progressService.daily(requireUserId(jwt)));
    }

    private String requireUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaimAsString("sub") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return jwt.getClaimAsString("sub");
    }
}
