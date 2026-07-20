package com.jpd.web.service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
@Slf4j
public class R2StorageService {

    private final S3Client r2Client;
    private final S3Presigner r2Presigner;
    private final String bucketName;

    public R2StorageService(
            S3Client r2Client,
            S3Presigner r2Presigner,
            @Value("${cloudflare.r2.bucket-name}") String bucketName) {
        this.r2Client = r2Client;
        this.r2Presigner = r2Presigner;
        this.bucketName = bucketName;
    }

    /**
     * Liệt kê tất cả object trong bucket theo prefix
     */
    public List<S3Object> listObjects(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        return r2Client.listObjectsV2(request).contents();
    }

    /**
     * Sinh presigned URL để frontend phát video trực tiếp mà không cần public bucket.
     * URL có thời hạn (expiryMinutes) để tránh lộ link vĩnh viễn.
     *
     * @param objectKey   key của object trên R2 bucket
     * @param expiryMinutes thời hạn URL (phút)
     * @return presigned URL string
     */
    public String generatePresignedUrl(String objectKey, int expiryMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return r2Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Lỗi tạo presigned URL cho object: {}", objectKey, e);
            return null;
        }
    }

    /**
     * Liệt kê các file video (lọc theo extension phổ biến)
     */
    public List<S3Object> listVideoFiles(String prefix) {
        return listObjects(prefix).stream()
                .filter(obj -> {
                    String key = obj.key().toLowerCase();
                    return key.endsWith(".mp4") || key.endsWith(".webm")
                            || key.endsWith(".mov") || key.endsWith(".avi")
                            || key.endsWith(".mkv");
                })
                .collect(Collectors.toList());
    }
}
