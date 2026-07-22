package com.jpd.web.controller.customer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
 // Cho phép CORS từ tất cả các nguồn
@RequestMapping("/api/tts")
@Tag(name = "Text to speech", description = "Pronunciation audio, proxied from Google Translate's TTS endpoint. Public — no authentication required.")
public class TtsController {

    @Operation(
        summary = "Stream spoken audio for a piece of text",
        description = """
            Proxies Google Translate's TTS endpoint and streams the resulting MP3 straight back to the
            caller. The response is `audio/mpeg` with `Cache-Control: public, max-age=86400`, so it can be
            used directly as an `<audio>` source.

            Public endpoint — no bearer token required.

            Because the audio is streamed, the 200 status and headers are committed before the upstream
            request is read. If Google fails or rejects the request mid-stream, the error is swallowed and
            the caller receives a 200 with a truncated or empty body — check that you actually got audio
            bytes rather than trusting the status code alone.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MP3 audio stream. May be empty or truncated if the upstream request failed after the response was committed.",
            content = @Content(mediaType = "audio/mpeg", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "500", description = "The upstream URL could not be built or opened. Empty body.", content = @Content)
    })
    @GetMapping()
    public ResponseEntity<StreamingResponseBody> textToSpeech(
            @Parameter(description = "Text to speak. Long inputs may be truncated by the upstream service.",
                    required = true, example = "今日はいい天気ですね")
            @RequestParam String text,
            @Parameter(description = "BCP-47 language tag passed to the upstream service as `tl`.", example = "ja-JP")
            @RequestParam(defaultValue = "ja-JP") String lang) {

        try { 
            // Mã hóa văn bản cho URL
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String googleTtsUrl = "https://translate.google.com/translate_tts?ie=UTF-8" +
                    "&q=" + encodedText +
                    "&tl=" + lang +
                    "&client=tw-ob";

            // Tạo kết nối HTTP
            URL url = new URL(googleTtsUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", 
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // Thiết lập response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
            headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");

            // Tạo streaming response
            StreamingResponseBody responseBody = outputStream -> {
                try {
                    // Đọc từ kết nối và ghi vào output stream
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    connection.disconnect();
                }
            };

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}