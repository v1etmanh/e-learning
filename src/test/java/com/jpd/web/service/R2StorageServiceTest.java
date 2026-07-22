package com.jpd.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2StorageServiceTest {

    @Mock
    private S3Client r2Client;
    @Mock
    private S3Presigner r2Presigner;

    private R2StorageService r2StorageService;

    @BeforeEach
    void setUp() {
        r2StorageService = new R2StorageService(r2Client, r2Presigner, "test-bucket");
    }

    @Test
    void listObjects_usesConfiguredBucketAndGivenPrefix() {
        S3Object obj = S3Object.builder().key("videos/a.mp4").build();
        when(r2Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(obj).build());

        List<S3Object> result = r2StorageService.listObjects("videos/");

        assertThat(result).containsExactly(obj);
        org.mockito.ArgumentCaptor<ListObjectsV2Request> captor = org.mockito.ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(r2Client).listObjectsV2(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().prefix()).isEqualTo("videos/");
    }

    @Nested
    class GeneratePresignedUrl {

        @Test
        void returnsUrl_onSuccess() throws Exception {
            PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
            when(presigned.url()).thenReturn(new URL("https://r2.example.com/signed?x=1"));
            when(r2Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

            String url = r2StorageService.generatePresignedUrl("videos/a.mp4", 5);

            assertThat(url).isEqualTo("https://r2.example.com/signed?x=1");
        }

        @Test
        void swallowsException_returnsNull_ratherThanThrowing() {
            when(r2Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenThrow(new RuntimeException("presign failed"));

            String url = r2StorageService.generatePresignedUrl("videos/a.mp4", 5);

            assertThat(url).isNull();
        }
    }

    @Test
    void listVideoFiles_filtersByVideoExtensionCaseInsensitive() {
        S3Object mp4 = S3Object.builder().key("videos/a.MP4").build();
        S3Object txt = S3Object.builder().key("videos/notes.txt").build();
        S3Object mkv = S3Object.builder().key("videos/b.mkv").build();
        when(r2Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(mp4, txt, mkv).build());

        List<S3Object> result = r2StorageService.listVideoFiles("videos/");

        assertThat(result).containsExactlyInAnyOrder(mp4, mkv);
    }
}
