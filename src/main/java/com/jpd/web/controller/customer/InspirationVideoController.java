package com.jpd.web.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpd.web.dto.TodayVideosResponse;
import com.jpd.web.service.InspirationVideoService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inspiration-videos")
@Slf4j
public class InspirationVideoController {

    private final InspirationVideoService inspirationVideoService;

    public InspirationVideoController(InspirationVideoService inspirationVideoService) {
        this.inspirationVideoService = inspirationVideoService;
    }

    /**
     * GET /api/inspiration-videos/today
     * Trả về TẤT CẢ video từ R2 bucket kèm presigned URL
     */
    @GetMapping("/today")
    public ResponseEntity<TodayVideosResponse> getAllVideos() {
        TodayVideosResponse response = inspirationVideoService.getAllVideos();
        return ResponseEntity.ok(response);
    }
}
