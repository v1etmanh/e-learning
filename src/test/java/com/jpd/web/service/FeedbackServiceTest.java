package com.jpd.web.service;

import com.jpd.web.exception.ExceedLimitRequestException;
import com.jpd.web.exception.FeedBackIligalException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.FeedbackRepository;
import com.jpd.web.service.utils.CommentFilterService;
import com.jpd.web.service.utils.CourseMetricsHelper;
import com.jpd.web.testutil.EnrollmentTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseMetricsHelper courseMetricsHelper;
    @Mock
    private CommentFilterService commentFilterService;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void throwsUnauthorized_whenNotEnrolled() {
        when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> feedbackService.addFeedback("student-1", 1L, "great course", 5));
    }

    @Test
    void throwsExceedLimit_whenAlreadyFedBack_beforeToxicityCheck() {
        Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
        enrollment.setFeedback(Feedback.builder().feedbackId(99L).build());
        when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                .thenReturn(Optional.of(enrollment));

        assertThrows(ExceedLimitRequestException.class,
                () -> feedbackService.addFeedback("student-1", 1L, "great course", 5));

        verify(commentFilterService, never()).isToxic(anyString());
    }

    @Test
    void throwsFeedBackIllegal_whenContentToxic_noSaveOrIncrement() {
        Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
        when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                .thenReturn(Optional.of(enrollment));
        when(commentFilterService.isToxic("bad content")).thenReturn(true);

        assertThrows(FeedBackIligalException.class,
                () -> feedbackService.addFeedback("student-1", 1L, "bad content", 5));

        verify(feedbackRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(courseMetricsHelper, never()).incrementRating(anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void savesFeedback_andIncrementsRatingWithExactRate() {
        Enrollment enrollment = EnrollmentTestDataBuilder.anEnrollment().build();
        when(enrollmentRepository.findByCourse_CourseIdAndCustomerId(1L, "student-1"))
                .thenReturn(Optional.of(enrollment));
        when(commentFilterService.isToxic("great course")).thenReturn(false);

        feedbackService.addFeedback("student-1", 1L, "great course", 4);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("great course");
        assertThat(captor.getValue().getRate()).isEqualTo(4);
        assertThat(captor.getValue().getEnrollment()).isSameAs(enrollment);

        verify(courseMetricsHelper).incrementRating(1L, 4);
    }
}
