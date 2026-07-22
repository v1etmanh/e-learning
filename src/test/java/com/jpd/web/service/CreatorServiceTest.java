package com.jpd.web.service;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Status;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatorServiceTest {

    @Mock
    private FireBaseService fireBaseService;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CreatorRepository creatorRepository;

    @InjectMocks
    private CreatorService creatorService;

    @Test
    void getAccount_propagatesNoSuchElement_whenCreatorMissing_noOrElseThrowGuard() {
        when(creatorRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> creatorService.getAccount("missing"));
    }

    @Test
    void getAccount_returnsMappedDto_whenCreatorExists() {
        Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
        when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

        CreatorDto result = creatorService.getAccount("creator-1");

        assertThat(result.getFullName()).isEqualTo(creator.getFullName());
    }

    @Nested
    class RemoveCertificate {

        @Test
        void throwsUnauthorized_whenUrlNotInCreatorsList() {
            Creator creator = CreatorTestDataBuilder.aCreatorWithCertificates(List.of("https://a.com/cert1.png"));
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            assertThrows(UnauthorizedException.class,
                    () -> creatorService.removeCertificate("creator-1", "https://a.com/other.png"));
        }

        @Test
        void deletesFromFirebase_whenUrlOwnedByCreator() {
            Creator creator = CreatorTestDataBuilder.aCreatorWithCertificates(List.of("https://a.com/cert1.png"));
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            creatorService.removeCertificate("creator-1", "https://a.com/cert1.png");

            org.mockito.Mockito.verify(fireBaseService).deleteImgByUrl("https://a.com/cert1.png");
        }
    }

    @Nested
    class UploadCertificate {

        @Test
        void appendsUrl_andResetsStatusToPending() throws Exception {
            Creator creator = CreatorTestDataBuilder.aCreator().certificateUrl(new ArrayList<>()).status(Status.SUCCESS).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));
            MockMultipartFile file = new MockMultipartFile("cert", "cert.png", "image/png", new byte[]{1});
            when(fireBaseService.uploadGlobalCertificate(file, "creator-1")).thenReturn("https://a.com/new-cert.png");

            creatorService.upLoadCertificate("creator-1", file);

            assertThat(creator.getCertificateUrl()).contains("https://a.com/new-cert.png");
            assertThat(creator.getStatus()).isEqualTo(Status.PENDING);
        }

        @Test
        void wrapsIOException_asFileUploadException() throws Exception {
            Creator creator = CreatorTestDataBuilder.aCreator().certificateUrl(new ArrayList<>()).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));
            MockMultipartFile file = new MockMultipartFile("cert", "cert.png", "image/png", new byte[]{1});
            when(fireBaseService.uploadGlobalCertificate(file, "creator-1")).thenThrow(new IOException("upload failed"));

            assertThrows(FileUploadException.class, () -> creatorService.upLoadCertificate("creator-1", file));
        }
    }
}
