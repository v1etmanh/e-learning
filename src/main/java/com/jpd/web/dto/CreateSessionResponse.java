package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CreateSessionResponse", description = "A newly opened quiz session. Share the PIN or the join URL with learners.")
public class CreateSessionResponse {
    @Schema(description = "Six-digit PIN learners type to join.", example = "482913")
    private String sessionCode;
    @Schema(description = "Internal UUID of the session.", example = "3f1a9c22-77bd-4e0a-9b41-5c2d8e6f0a11")
    private String sessionId;
    @Schema(description = "URL of a QR code encoding the join URL.", example = "https://api.qrserver.com/v1/create-qr-code/?data=http://localhost:3000/quiz/join/482913")
    private String qrCodeUrl;
    @Schema(description = "Direct link learners can open to join.", example = "http://localhost:3000/quiz/join/482913")
    private String joinUrl;
    @Schema(description = "Title of the quiz.", example = "On tap Kanji N5")
    private String title;
    @Schema(description = "Number of playable questions in the session.", example = "15")
    private Integer totalQuestions;
}