package com.jpd.web.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.PendingImgRepository;
import com.jpd.web.service.utils.FileCategory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FireBaseServiceTest {

    @Mock
    private StorageClient storageClient;
    @Mock
    private Bucket bucket;
    @Mock
    private Blob blob;
    @Mock
    private PendingImgRepository pendingImgRepository;

    private FireBaseService fireBaseService;

    @BeforeEach
    void setUp() {
        fireBaseService = new FireBaseService();
        ReflectionTestUtils.setField(fireBaseService, "storageClient", storageClient);
        ReflectionTestUtils.setField(fireBaseService, "pendingImageRepository", pendingImgRepository);
    }

    @Nested
    class UploadFile {

        @Test
        void buildsPathAndPublicUrl_fromBucketNameAndTimestamp() throws Exception {
            when(storageClient.bucket()).thenReturn(bucket);
            when(bucket.getName()).thenReturn("my-bucket");
            when(bucket.create(any(String.class), any(byte[].class), any(String.class))).thenReturn(blob);
            MockMultipartFile file = new MockMultipartFile("f", "photo.png", "image/png", new byte[]{1, 2});

            String url = fireBaseService.uploadFile(file, TypeOfFile.IMG);

            assertThat(url).startsWith("https://firebasestorage.googleapis.com/v0/b/my-bucket/o/IMG%2F");
            assertThat(url).contains("_photo.png?alt=media");
        }

        @Test
        void wrapsAnyException_asFileUploadException() {
            when(storageClient.bucket()).thenThrow(new RuntimeException("gcs down"));
            MockMultipartFile file = new MockMultipartFile("f", "photo.png", "image/png", new byte[]{1});

            assertThrows(FileUploadException.class, () -> fireBaseService.uploadFile(file, TypeOfFile.IMG));
        }
    }

    @Nested
    class DeleteImgByUrl {

        @Test
        void returnsNotFoundMessage_ratherThanThrowing_whenBlobMissing() {
            when(storageClient.bucket()).thenReturn(bucket);
            when(bucket.getName()).thenReturn("my-bucket");
            when(bucket.get("IMG/file.png")).thenReturn(null);

            String result = fireBaseService.deleteImgByUrl(
                    "https://firebasestorage.googleapis.com/v0/b/my-bucket/o/IMG%2Ffile.png?alt=media");

            assertThat(result).contains("File not found");
        }

        @Test
        void deletesBlob_whenFound() {
            when(storageClient.bucket()).thenReturn(bucket);
            when(bucket.getName()).thenReturn("my-bucket");
            when(bucket.get("IMG/file.png")).thenReturn(blob);
            when(blob.delete()).thenReturn(true);

            String result = fireBaseService.deleteImgByUrl(
                    "https://firebasestorage.googleapis.com/v0/b/my-bucket/o/IMG%2Ffile.png?alt=media");

            assertThat(result).contains("deleted successfully");
        }
    }

    @Test
    void cleanPendingImages_deletesEachPendingImage_thenRemovesFromRepository() {
        FireBaseService spyService = spy(fireBaseService);
        PendingImage img1 = new PendingImage(1L, "creator-1", "url1", Status.PENDING, null);
        PendingImage img2 = new PendingImage(2L, "creator-1", "url2", Status.PENDING, null);
        when(pendingImgRepository.findByStatus(Status.PENDING)).thenReturn(List.of(img1, img2));
        doReturn("ok").when(spyService).deleteImgByUrl(anyString());

        spyService.cleanPendingImages();

        verify(spyService).deleteImgByUrl("url1");
        verify(spyService).deleteImgByUrl("url2");
        verify(pendingImgRepository).delete(img1);
        verify(pendingImgRepository).delete(img2);
    }

    @Nested
    class GetFileFromUrl {

        @Test
        void blankUrl_isRejected_butWrappedInIOExceptionByTheOuterCatch() {
            // getFileFromUrl throws IllegalArgumentException for a blank URL internally,
            // but the method's own try/catch(Exception) wraps everything - including that
            // validation error - into an IOException before it reaches the caller.
            java.io.IOException ex = assertThrows(java.io.IOException.class,
                    () -> fireBaseService.getFileFromUrl(""));
            assertThat(ex.getMessage()).contains("URL không hợp lệ");
        }

        @Test
        void wrapsIllegalArgument_asIOException_whenUrlNotFromFirebase() {
            when(storageClient.bucket()).thenReturn(bucket);
            when(bucket.getName()).thenReturn("my-bucket");

            assertThrows(java.io.IOException.class,
                    () -> fireBaseService.getFileFromUrl("https://example.com/not-firebase.png"));
        }

        @Test
        void returnsBlobContent_onHappyPath() throws Exception {
            when(storageClient.bucket()).thenReturn(bucket);
            when(bucket.getName()).thenReturn("my-bucket");
            when(bucket.get("IMG/file.png")).thenReturn(blob);
            when(blob.getContent()).thenReturn(new byte[]{9, 8, 7});

            byte[] result = fireBaseService.getFileFromUrl(
                    "https://firebasestorage.googleapis.com/v0/b/my-bucket/o/IMG%2Ffile.png?alt=media");

            assertThat(result).containsExactly(9, 8, 7);
        }
    }

    @Test
    void getCustomerFiles_filtersOutPlaceholderAndFolderBlobs() {
        when(storageClient.bucket()).thenReturn(bucket);
        when(bucket.getName()).thenReturn("my-bucket");
        Blob realFile = mockBlobNamed("customerFile/c1/certificate/cert.png");
        Blob placeholder = mockBlobNamed("customerFile/c1/certificate/.placeholder");
        Blob folder = mockBlobNamed("customerFile/c1/certificate/subfolder/");
        Page<Blob> page = pageOf(List.of(realFile, placeholder, folder));
        when(bucket.list(any(com.google.cloud.storage.Storage.BlobListOption.class))).thenReturn(page);

        List<String> result = fireBaseService.getCustomerFiles("c1", FileCategory.CERTIFICATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("cert.png");
    }

    private Blob mockBlobNamed(String name) {
        Blob b = org.mockito.Mockito.mock(Blob.class);
        when(b.getName()).thenReturn(name);
        return b;
    }

    @SuppressWarnings("unchecked")
    private Page<Blob> pageOf(List<Blob> blobs) {
        Page<Blob> page = org.mockito.Mockito.mock(Page.class);
        when(page.iterateAll()).thenReturn(blobs);
        return page;
    }
}
