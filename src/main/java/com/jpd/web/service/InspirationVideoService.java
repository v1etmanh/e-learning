package com.jpd.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.InspirationVideoDTO;
import com.jpd.web.dto.TodayVideosResponse;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@Slf4j
public class InspirationVideoService {

    private final R2StorageService r2StorageService;

    @Value("${cloudflare.r2.presigned-url-expiry-minutes:120}")
    private int presignedUrlExpiryMinutes;

    @Value("${cloudflare.r2.video-prefix}")
    private String videoPrefix;

    public InspirationVideoService(R2StorageService r2StorageService) {
        this.r2StorageService = r2StorageService;
    }

    /**
     * Load TẤT CẢ video từ R2 bucket, trả presigned URL cho frontend.
     * Không cần DB, không cần xoay vòng — đơn giản scan R2 rồi trả về.
     */
    public TodayVideosResponse getAllVideos() {
        log.info("Loading all videos from R2 prefix: {}", videoPrefix);

        List<S3Object> videoFiles = r2StorageService.listVideoFiles(videoPrefix);

        List<InspirationVideoDTO> dtos = videoFiles.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.info("Loaded {} videos from R2", dtos.size());

        return TodayVideosResponse.builder()
                .date(java.time.LocalDate.now().toString())
                .videos(dtos)
                .build();
    }

    /**
     * Convert S3Object → DTO với presigned URL
     */
    private InspirationVideoDTO toDTO(S3Object obj) {
        String objectKey = obj.key();
        String videoUrl = r2StorageService.generatePresignedUrl(objectKey, presignedUrlExpiryMinutes);

        return InspirationVideoDTO.builder()
                .id(objectKey)  // dùng object key làm ID
                .title(extractTitleFromKey(objectKey))
                .videoUrl(videoUrl)
                .description("Inspiration video")
                .speaker("")
                .topic("motivation")
                .difficultyLevel("intermediate")
                .build();
    }

    /**
     * Trích xuất tiêu đề từ R2 object key
     */
    private String extractTitleFromKey(String objectKey) {
        String fileName = objectKey;
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        fileName = fileName.replace("_", " ").replace("-", " ");
        return fileName;
    }
}
