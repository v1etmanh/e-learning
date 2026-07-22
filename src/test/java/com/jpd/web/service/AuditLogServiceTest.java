package com.jpd.web.service;

import com.jpd.web.model.AuditLog;
import com.jpd.web.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void logAction_savesAllFourFields() {
        auditLogService.logAction("BAN_CREATOR", "creator-1", "admin@x.com", "policy violation");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActionType()).isEqualTo("BAN_CREATOR");
        assertThat(saved.getTargetCreatorId()).isEqualTo("creator-1");
        assertThat(saved.getAdminEmail()).isEqualTo("admin@x.com");
        assertThat(saved.getReason()).isEqualTo("policy violation");
    }

    @Test
    void getLogsByCreator_delegatesToRepository() {
        AuditLog log = AuditLog.builder().targetCreatorId("creator-1").build();
        when(auditLogRepository.findByTargetCreatorIdOrderByTimestampDesc("creator-1")).thenReturn(List.of(log));

        List<AuditLog> result = auditLogService.getLogsByCreator("creator-1");

        assertThat(result).containsExactly(log);
    }

    @Test
    void getLogsByAdmin_delegatesToRepository() {
        AuditLog log = AuditLog.builder().adminEmail("admin@x.com").build();
        when(auditLogRepository.findByAdminEmailOrderByTimestampDesc("admin@x.com")).thenReturn(List.of(log));

        List<AuditLog> result = auditLogService.getLogsByAdmin("admin@x.com");

        assertThat(result).containsExactly(log);
    }
}
