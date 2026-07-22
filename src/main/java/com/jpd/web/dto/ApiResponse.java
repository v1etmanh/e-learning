package com.jpd.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Envelope used by the admin course endpoints. Note the admin dashboard uses a different envelope of the same name from the `controller.admin` package.")
public class ApiResponse<T> {
    @Schema(description = "Whether the operation succeeded.", example = "true")
    private boolean success;
    @Schema(description = "The payload. Null on failure.")
    private T data;
    @Schema(description = "Failure message. Null on success.", example = "null")
    private String error;
    @Schema(description = "When the response was built.", example = "2026-07-22T09:15:30.412")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now());
    }
}


