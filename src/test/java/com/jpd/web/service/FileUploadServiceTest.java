package com.jpd.web.service;

import com.jpd.web.exception.FileUploadException;
import com.jpd.web.model.CreatorMediaCapacity;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.repository.CreatorMediaCapacityRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.PendingImgRepository;
import com.jpd.web.service.utils.FileCategory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private PendingImgRepository pendingImgRepository;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private FireBaseService fireBaseService;
    @Mock
    private CreatorMediaCapacityRepository creatorMediaCapacityRepository;

    @InjectMocks
    private FileUploadService fileUploadService;

    private static final long ONE_MB = 1024 * 1024L;

    @Nested
    class SaveImgIntoFirebase {

        @Test
        void throwsIllegalArgument_whenFileNullOrEmpty() {
            assertThrows(IllegalArgumentException.class,
                    () -> fileUploadService.saveImgIntoFirebase("creator-1", null, TypeOfFile.IMG));
        }

        @Test
        void throwsIllegalArgument_whenTypeUnrecognized() {
            MockMultipartFile file = new MockMultipartFile("f", "a.png", "image/png", new byte[]{1});

            assertThrows(IllegalArgumentException.class,
                    () -> fileUploadService.saveImgIntoFirebase("creator-1", file, TypeOfFile.CERTIFICATE));
        }

        @Test
        void throwsIllegalArgument_whenSingleFileOver50MB() {
            byte[] tooBig = new byte[(int) (50 * ONE_MB) + 1];
            MockMultipartFile file = new MockMultipartFile("f", "a.png", "image/png", tooBig);

            assertThrows(IllegalArgumentException.class,
                    () -> fileUploadService.saveImgIntoFirebase("creator-1", file, TypeOfFile.IMG));
        }

        @Test
        void createsNewCapacityRecord_withZeroCapacity_whenNoneExists() throws Exception {
            when(creatorMediaCapacityRepository.findByCreatorId("creator-1")).thenReturn(null);
            when(fireBaseService.uploadFileByCustomer(anyString(), any(FileCategory.class), any())).thenReturn("https://url");
            MockMultipartFile file = new MockMultipartFile("f", "a.png", "image/png", new byte[]{1, 2, 3});

            fileUploadService.saveImgIntoFirebase("creator-1", file, TypeOfFile.IMG);

            org.mockito.ArgumentCaptor<CreatorMediaCapacity> captor = org.mockito.ArgumentCaptor.forClass(CreatorMediaCapacity.class);
            verify(creatorMediaCapacityRepository).save(captor.capture());
            assertThat(captor.getValue().getCapacity()).isEqualTo(3L);
        }

        @Test
        void throwsIllegalArgument_whenSumExceeds500MbFolderCap() {
            CreatorMediaCapacity existing = CreatorMediaCapacity.builder()
                    .creatorId("creator-1").capacity(500 * ONE_MB - 10).build();
            when(creatorMediaCapacityRepository.findByCreatorId("creator-1")).thenReturn(existing);
            byte[] pushesOver = new byte[20];
            MockMultipartFile file = new MockMultipartFile("f", "a.png", "image/png", pushesOver);

            assertThrows(IllegalArgumentException.class,
                    () -> fileUploadService.saveImgIntoFirebase("creator-1", file, TypeOfFile.IMG));
        }

        @Test
        void allowsUpload_whenSumExactlyAtFolderCap() throws Exception {
            long existingCapacity = 500 * ONE_MB - 10;
            CreatorMediaCapacity existing = CreatorMediaCapacity.builder()
                    .creatorId("creator-1").capacity(existingCapacity).build();
            when(creatorMediaCapacityRepository.findByCreatorId("creator-1")).thenReturn(existing);
            when(fireBaseService.uploadFileByCustomer(anyString(), any(FileCategory.class), any())).thenReturn("https://url");
            MockMultipartFile file = new MockMultipartFile("f", "a.png", "image/png", new byte[10]);

            String url = fileUploadService.saveImgIntoFirebase("creator-1", file, TypeOfFile.IMG);

            assertThat(url).isEqualTo("https://url");
        }

        @Test
        void happyPath_updatesCapacity_thenUploadsToFirebase() throws Exception {
            when(creatorMediaCapacityRepository.findByCreatorId("creator-1")).thenReturn(null);
            when(fireBaseService.uploadFileByCustomer(eq("creator-1"), eq(FileCategory.IMG), any())).thenReturn("https://url/img.png");
            MockMultipartFile file = new MockMultipartFile("f", "a.png", "image/png", new byte[]{1, 2});

            String url = fileUploadService.saveImgIntoFirebase("creator-1", file, TypeOfFile.IMG);

            assertThat(url).isEqualTo("https://url/img.png");
        }
    }

    @Nested
    class DeleteFileByUrl {

        @Test
        void throwsFileUploadException_whenCreatorMissing() {
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.empty());

            assertThrows(FileUploadException.class,
                    () -> fileUploadService.deleteFileByUrl("https://url", "creator-1"));
        }

        @Test
        void capacityNeverDecremented_becauseFileSizeIsHardcodedToZero() {
            // Documents current (likely incomplete) behavior: fileSize is hardcoded to 0
            // and only conditionally decremented `if (fileSize > 0)`, which never fires -
            // storage quota is never freed on delete today.
            com.jpd.web.model.Creator creator = com.jpd.web.testutil.CreatorTestDataBuilder.aCreatorWithId("creator-1");
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));
            PendingImage pending = new PendingImage(1L, "creator-1", "https://url", Status.PENDING, null);
            when(pendingImgRepository.findByCreatorIdAndUrl("creator-1", "https://url")).thenReturn(Optional.of(pending));
            when(fireBaseService.deleteImgByUrl("https://url")).thenReturn("deleted");

            fileUploadService.deleteFileByUrl("https://url", "creator-1");

            verify(pendingImgRepository).delete(pending);
            verify(creatorMediaCapacityRepository, never()).save(any());
        }
    }

    @Nested
    class CanUploadFile {

        @Test
        void returnsFalse_whenNoCapacityRecord() {
            when(creatorMediaCapacityRepository.findByCreatorId("creator-1")).thenReturn(null);

            assertThat(fileUploadService.canUploadFile("creator-1", ONE_MB)).isFalse();
        }

        @Test
        void returnsTrue_whenExactlyAtCapBoundary() {
            CreatorMediaCapacity capacity = CreatorMediaCapacity.builder()
                    .creatorId("creator-1").capacity(499 * ONE_MB).build();
            when(creatorMediaCapacityRepository.findByCreatorId("creator-1")).thenReturn(capacity);

            assertThat(fileUploadService.canUploadFile("creator-1", ONE_MB)).isTrue();
        }

        @Test
        void returnsFalse_whenOverCapBoundary() {
            CreatorMediaCapacity capacity = CreatorMediaCapacity.builder()
                    .creatorId("creator-1").capacity(499 * ONE_MB + 1).build();
            when(creatorMediaCapacityRepository.findByCreatorId("creator-1")).thenReturn(capacity);

            assertThat(fileUploadService.canUploadFile("creator-1", ONE_MB)).isFalse();
        }
    }
}
