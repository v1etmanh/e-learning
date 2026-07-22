package com.jpd.web.service;

import com.jpd.web.dto.TodayVideosResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspirationVideoServiceTest {

    @Mock
    private R2StorageService r2StorageService;

    private InspirationVideoService inspirationVideoService;

    @BeforeEach
    void setUp() {
        inspirationVideoService = new InspirationVideoService(r2StorageService);
        ReflectionTestUtils.setField(inspirationVideoService, "presignedUrlExpiryMinutes", 60);
        ReflectionTestUtils.setField(inspirationVideoService, "videoPrefix", "videos/");
    }

    @Test
    void getAllVideos_presignsOncePerVideo() {
        S3Object v1 = S3Object.builder().key("videos/a.mp4").build();
        S3Object v2 = S3Object.builder().key("videos/b.mp4").build();
        when(r2StorageService.listVideoFiles("videos/")).thenReturn(List.of(v1, v2));
        when(r2StorageService.generatePresignedUrl(any(String.class), eq(60))).thenReturn("https://signed");

        TodayVideosResponse result = inspirationVideoService.getAllVideos();

        assertThat(result.getVideos()).hasSize(2);
        verify(r2StorageService).generatePresignedUrl("videos/a.mp4", 60);
        verify(r2StorageService).generatePresignedUrl("videos/b.mp4", 60);
    }

    @Nested
    class ExtractTitleFromKey {

        private String titleFor(String key) {
            S3Object obj = S3Object.builder().key(key).build();
            when(r2StorageService.listVideoFiles("videos/")).thenReturn(List.of(obj));
            when(r2StorageService.generatePresignedUrl(any(String.class), eq(60))).thenReturn("https://signed");
            return inspirationVideoService.getAllVideos().getVideos().get(0).getTitle();
        }

        @Test
        void stripsPathAndExtension_replacesUnderscoresAndDashesWithSpaces() {
            assertThat(titleFor("videos/my_cool-video.mp4")).isEqualTo("my cool video");
        }

        @Test
        void handlesKeyWithNoSlash_rootLevelObject() {
            assertThat(titleFor("standalone.mp4")).isEqualTo("standalone");
        }

        @Test
        void handlesKeyWithNoExtension() {
            assertThat(titleFor("videos/noextension")).isEqualTo("noextension");
        }
    }
}
