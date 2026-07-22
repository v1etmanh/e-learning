package com.jpd.web.service.utils;

import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.model.CourseMetrics;
import com.jpd.web.repository.CourseMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseMetricsHelperTest {

    @Mock
    private CourseMetricsRepository courseMetricsRepository;

    private CourseMetricsHelper courseMetricsHelper;

    @BeforeEach
    void setUp() {
        courseMetricsHelper = new CourseMetricsHelper(courseMetricsRepository);
    }

    private CourseMetrics metrics(int totalStudents, int totalRating, double averageRating,
                                   int totalPeopleLike, int totalReport) {
        return CourseMetrics.builder()
                .courseDescriptionId(1L)
                .totalStudents(totalStudents)
                .totalRating(totalRating)
                .averageRating(averageRating)
                .totalPeopleLike(totalPeopleLike)
                .totalReport(totalReport)
                .build();
    }

    @Test
    void incrementEnrollmentCount_throwsCourseNotFound_whenMetricsRowMissing() {
        when(courseMetricsRepository.findByCourse_CourseId(1L)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> courseMetricsHelper.incrementEnrollmentCount(1L));
    }

    @Test
    void incrementEnrollmentCount_incrementsStudentsAndTimestamp() {
        CourseMetrics existing = metrics(5, 0, 0.0, 0, 0);
        when(courseMetricsRepository.findByCourse_CourseId(1L)).thenReturn(Optional.of(existing));

        LocalDateTime before = LocalDateTime.now();
        courseMetricsHelper.incrementEnrollmentCount(1L);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<CourseMetrics> captor = ArgumentCaptor.forClass(CourseMetrics.class);
        verify(courseMetricsRepository).save(captor.capture());
        CourseMetrics saved = captor.getValue();

        assertThat(saved.getTotalStudents()).isEqualTo(6);
        assertThat(saved.getLastEnrollmentAt()).isBetween(before, after);
    }

    @Test
    void incrementRating_appliesWeightedAverageFormula() {
        // avg=4.0, totalRating=3, new rating=5 -> (4*3+5)/4 = 4.25
        CourseMetrics existing = metrics(0, 3, 4.0, 0, 0);
        when(courseMetricsRepository.findByCourse_CourseId(1L)).thenReturn(Optional.of(existing));

        courseMetricsHelper.incrementRating(1L, 5);

        ArgumentCaptor<CourseMetrics> captor = ArgumentCaptor.forClass(CourseMetrics.class);
        verify(courseMetricsRepository).save(captor.capture());
        CourseMetrics saved = captor.getValue();

        assertThat(saved.getAverageRating()).isCloseTo(4.25, within(1e-9));
        assertThat(saved.getTotalRating()).isEqualTo(4);
        assertThat(saved.getLastRatingAt()).isNotNull();
    }

    @Test
    void incrementRating_firstRating_collapsesToNewRatingValue() {
        CourseMetrics existing = metrics(0, 0, 0.0, 0, 0);
        when(courseMetricsRepository.findByCourse_CourseId(1L)).thenReturn(Optional.of(existing));

        courseMetricsHelper.incrementRating(1L, 5);

        ArgumentCaptor<CourseMetrics> captor = ArgumentCaptor.forClass(CourseMetrics.class);
        verify(courseMetricsRepository).save(captor.capture());
        CourseMetrics saved = captor.getValue();

        assertThat(saved.getAverageRating()).isCloseTo(5.0, within(1e-9));
        assertThat(saved.getTotalRating()).isEqualTo(1);
    }

    @Test
    void incrementReport_incrementsCountAndTimestamp() {
        CourseMetrics existing = metrics(0, 0, 0.0, 0, 2);
        when(courseMetricsRepository.findByCourse_CourseId(1L)).thenReturn(Optional.of(existing));

        courseMetricsHelper.incrementReport(1L);

        ArgumentCaptor<CourseMetrics> captor = ArgumentCaptor.forClass(CourseMetrics.class);
        verify(courseMetricsRepository).save(captor.capture());
        CourseMetrics saved = captor.getValue();

        assertThat(saved.getTotalReport()).isEqualTo(3);
        assertThat(saved.getLastReportAt()).isNotNull();
    }

    @Test
    void incrementWishlist_incrementsCountAndTimestamp() {
        CourseMetrics existing = metrics(0, 0, 0.0, 7, 0);
        when(courseMetricsRepository.findByCourse_CourseId(1L)).thenReturn(Optional.of(existing));

        courseMetricsHelper.incrementWishlist(1L);

        ArgumentCaptor<CourseMetrics> captor = ArgumentCaptor.forClass(CourseMetrics.class);
        verify(courseMetricsRepository).save(captor.capture());
        CourseMetrics saved = captor.getValue();

        assertThat(saved.getTotalPeopleLike()).isEqualTo(8);
        assertThat(saved.getLastLikeAt()).isNotNull();
    }
}
