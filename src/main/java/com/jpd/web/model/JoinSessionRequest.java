package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "JoinSessionRequest", description = "A learner's request to join a live quiz session.")
public class JoinSessionRequest {
    @Schema(description = "Six-digit PIN of the session to join.", example = "482913")
    private String sessionCode;
    @Schema(description = "Nickname shown on the leaderboard.", example = "Thuy")
    private String participantName;
}