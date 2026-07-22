package com.jpd.web.service;

import com.jpd.web.dto.AdminCreatorDetailDto;
import com.jpd.web.dto.AdminCreatorListDto;
import com.jpd.web.dto.CertificateApprovalDto;
import com.jpd.web.model.Creator;
import com.jpd.web.model.CreatorWarning;
import com.jpd.web.model.Status;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CreatorWarningRepository;
import com.jpd.web.repository.ReportRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.testutil.CreatorTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCreatorServiceTest {

    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private CreatorWarningRepository warningRepository;
    @Mock
    private ValidationResources validationResources;

    @InjectMocks
    private AdminCreatorService adminCreatorService;

    @Nested
    class GetCreatorList {

        @Test
        void filtersByStatus_whenStatusProvided() {
            when(creatorRepository.findAllByStatus(Status.PENDING))
                    .thenReturn(List.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));

            Page<AdminCreatorListDto> result = adminCreatorService.getCreatorList(Status.PENDING, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            verify(creatorRepository, org.mockito.Mockito.never()).findAll();
        }

        @Test
        void filtersByCaseInsensitiveNameSearch() {
            Creator matching = CreatorTestDataBuilder.aCreator().creatorId("c1").fullName("Alice Nguyen").build();
            Creator nonMatching = CreatorTestDataBuilder.aCreator().creatorId("c2").fullName("Bob Tran").build();
            when(creatorRepository.findAll()).thenReturn(List.of(matching, nonMatching));

            Page<AdminCreatorListDto> result = adminCreatorService.getCreatorList(null, "alice", 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getFullName()).isEqualTo("Alice Nguyen");
        }

        @Test
        void returnsEmptyPage_whenNoResults() {
            when(creatorRepository.findAll()).thenReturn(List.of());

            Page<AdminCreatorListDto> result = adminCreatorService.getCreatorList(null, "nobody", 0, 10);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void returnsPartialLastPage() {
            List<Creator> creators = List.of(
                    CreatorTestDataBuilder.aCreator().creatorId("c1").build(),
                    CreatorTestDataBuilder.aCreator().creatorId("c2").build(),
                    CreatorTestDataBuilder.aCreator().creatorId("c3").build());
            when(creatorRepository.findAll()).thenReturn(creators);

            Page<AdminCreatorListDto> result = adminCreatorService.getCreatorList(null, null, 1, 2);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }

    @Nested
    class GetCreatorDetail {

        @Test
        void throwsNpe_whenCreatorMissing_documentingUnguardedOptionalOrElseNull() {
            when(creatorRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(NullPointerException.class,
                    () -> adminCreatorService.getCreatorDetail("missing"));
        }

        @Test
        void returnsDetailDto_whenCreatorExists() {
            Creator creator = CreatorTestDataBuilder.aCreatorWithId("creator-1");
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            AdminCreatorDetailDto result = adminCreatorService.getCreatorDetail("creator-1");

            assertThat(result.getCreatorId()).isEqualTo("creator-1");
        }
    }

    @Test
    void getPendingCertificates_mapsAllPendingCreators() {
        when(creatorRepository.findAllByStatus(Status.PENDING))
                .thenReturn(List.of(CreatorTestDataBuilder.aCreatorWithId("creator-1")));

        List<CertificateApprovalDto> result = adminCreatorService.getPendingCertificates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCreatorId()).isEqualTo("creator-1");
    }

    @Nested
    class ApproveRejectCertificate {

        @Test
        void approve_throwsIllegalState_whenNotPending() {
            Creator creator = CreatorTestDataBuilder.aCreator().status(Status.SUCCESS).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            assertThrows(IllegalStateException.class,
                    () -> adminCreatorService.approveCertificate("creator-1", "admin@x.com", "ok"));
        }

        @Test
        void approve_setsSuccessStatus_andLogsAction() {
            Creator creator = CreatorTestDataBuilder.aCreator().status(Status.PENDING).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            adminCreatorService.approveCertificate("creator-1", "admin@x.com", "ok");

            assertThat(creator.getStatus()).isEqualTo(Status.SUCCESS);
            verify(auditLogService).logAction("APPROVE_CERT", "creator-1", "admin@x.com", "Certificate approved. Note: ok");
        }

        @Test
        void reject_throwsIllegalState_whenNotPending() {
            Creator creator = CreatorTestDataBuilder.aCreator().status(Status.SUCCESS).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            assertThrows(IllegalStateException.class,
                    () -> adminCreatorService.rejectCertificate("creator-1", "bad quality", "admin@x.com"));
        }

        @Test
        void reject_setsRejectedStatus_andLogsAction() {
            Creator creator = CreatorTestDataBuilder.aCreator().status(Status.PENDING).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            adminCreatorService.rejectCertificate("creator-1", "bad quality", "admin@x.com");

            assertThat(creator.getStatus()).isEqualTo(Status.REJECTED);
            verify(auditLogService).logAction("REJECT_CERT", "creator-1", "admin@x.com", "Certificate rejected. Reason: bad quality");
        }
    }

    @Nested
    class WarnCreator {

        private MockedStatic<LocalDateTime> mockedNow;
        private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 22, 12, 0);

        @BeforeEach
        void pinNow() {
            mockedNow = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
            mockedNow.when(LocalDateTime::now).thenReturn(NOW);
        }

        @AfterEach
        void unpinNow() {
            mockedNow.close();
        }

        @Test
        void doesNotSuspend_whenFewerThan3WarningsIn90Days() {
            Creator creator = CreatorTestDataBuilder.aCreator().status(Status.SUCCESS).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));
            when(warningRepository.countByCreatorAndIssuedAtAfter(creator, NOW.minusDays(90))).thenReturn(2L);

            adminCreatorService.warnCreator("creator-1", "spam", "admin@x.com");

            assertThat(creator.getStatus()).isEqualTo(Status.SUCCESS);
        }

        @Test
        void autoSuspends_when3OrMoreWarningsIn90Days() {
            Creator creator = CreatorTestDataBuilder.aCreator().status(Status.SUCCESS).build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            when(warningRepository.countByCreatorAndIssuedAtAfter(org.mockito.ArgumentMatchers.eq(creator), sinceCaptor.capture()))
                    .thenReturn(3L);

            adminCreatorService.warnCreator("creator-1", "spam", "admin@x.com");

            assertThat(creator.getStatus()).isEqualTo(Status.SUSPENDED);
            assertThat(sinceCaptor.getValue()).isEqualTo(NOW.minusDays(90));
        }
    }

    @Nested
    class BanUnbanCreator {

        @Test
        void temporaryBan_setsBannedUntilAndStatus() {
            Creator creator = CreatorTestDataBuilder.aCreator().build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            adminCreatorService.banCreator("creator-1", "violation", 7, "admin@x.com");

            assertThat(creator.isBan()).isTrue();
            assertThat(creator.getStatus()).isEqualTo(Status.BANNED);
            assertThat(creator.getBannedUntil()).isNotNull();
        }

        @Test
        void permanentBan_whenNoDuration_leavesBannedUntilNull() {
            Creator creator = CreatorTestDataBuilder.aCreator().build();
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            adminCreatorService.banCreator("creator-1", "violation", null, "admin@x.com");

            assertThat(creator.isBan()).isTrue();
            assertThat(creator.getStatus()).isEqualTo(Status.BANNED);
            assertThat(creator.getBannedUntil()).isNull();
        }

        @Test
        void ban_cascadesToAllOwnedCourses() {
            Creator creator = CreatorTestDataBuilder.aCreator().build();
            com.jpd.web.model.Course course1 = com.jpd.web.testutil.CourseTestDataBuilder.aCourse().courseId(1L).creator(creator).isBan(false).build();
            com.jpd.web.model.Course course2 = com.jpd.web.testutil.CourseTestDataBuilder.aCourse().courseId(2L).creator(creator).isBan(false).build();
            creator.setCourses(List.of(course1, course2));
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            adminCreatorService.banCreator("creator-1", "violation", null, "admin@x.com");

            assertThat(course1.isBan()).isTrue();
            assertThat(course2.isBan()).isTrue();
        }

        @Test
        void unban_resetsStatusAndUnbansAllCourses() {
            Creator creator = CreatorTestDataBuilder.aCreator().status(Status.BANNED).ban(true).build();
            com.jpd.web.model.Course course = com.jpd.web.testutil.CourseTestDataBuilder.aCourse().creator(creator).isBan(true).build();
            creator.setCourses(List.of(course));
            when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(creator));

            adminCreatorService.unbanCreator("creator-1", "resolved", "admin@x.com");

            assertThat(creator.isBan()).isFalse();
            assertThat(creator.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(creator.getBannedUntil()).isNull();
            assertThat(course.isBan()).isFalse();
        }
    }
}
