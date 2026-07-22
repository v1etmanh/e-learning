package com.jpd.web.service.utils;

import com.jpd.web.dto.NoticeForm;
import com.jpd.web.exception.EmailSendingFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendNoticeServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SendNoticeService sendNoticeService;

    private NoticeForm aNotice() {
        return NoticeForm.builder().message("Welcome!").createdAt(LocalDateTime.now()).emailSent(false).build();
    }

    @Test
    void happyPath_sendsEmail_andMarksEmailSentTrue() {
        NoticeForm notice = aNotice();

        sendNoticeService.sendNotice(notice, "user@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(notice.getEmailSent()).isTrue();
    }

    @Test
    void failurePath_setsEmailSentFalse_andThrowsEmailSendingFailedException() {
        NoticeForm notice = aNotice();
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(EmailSendingFailedException.class,
                () -> sendNoticeService.sendNotice(notice, "user@example.com"));

        assertThat(notice.getEmailSent()).isFalse();
    }
}
