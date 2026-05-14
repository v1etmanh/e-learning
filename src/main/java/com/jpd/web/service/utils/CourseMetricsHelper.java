package com.jpd.web.service.utils;

import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.model.CourseMetrics;
import com.jpd.web.repository.CourseMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CourseMetricsHelper {

    private final CourseMetricsRepository courseMetricsRepository;

    private CourseMetrics getByCourseIdOrThrow(Long courseId) {
        return courseMetricsRepository.findByCourse_CourseId(courseId)
                .orElseThrow(() -> new CourseNotFoundException  (
                        courseId));
    }
    public void incrementEnrollmentCount(Long courseId) {
        CourseMetrics c= getByCourseIdOrThrow(courseId);
        c.setTotalStudents(c.getTotalStudents() + 1);
        c.setLastEnrollmentAt(LocalDateTime.now());

        courseMetricsRepository.save(c);
    }
    public void  incrementRating(Long courseId,int rating)
    {
        CourseMetrics c= getByCourseIdOrThrow(courseId);


        c.setAverageRating((c.getAverageRating()*c.getTotalRating()+rating)/(c.getTotalRating()+1));
        c.setTotalRating(c.getTotalRating()+1);
        c.setLastRatingAt(LocalDateTime.now());
        courseMetricsRepository.save(c);
    }
    public void incrementReport(Long courseId) {
        CourseMetrics c= getByCourseIdOrThrow(courseId);
        c.setTotalReport(c.getTotalReport() + 1);
        c.setLastReportAt(LocalDateTime.now());
        courseMetricsRepository.save(c);
    }
    public void incrementWishlist(Long courseId) {
        CourseMetrics c= getByCourseIdOrThrow(courseId);
        c.setTotalPeopleLike(c.getTotalPeopleLike() + 1);
        c.setLastLikeAt(LocalDateTime.now());
        courseMetricsRepository.save(c);
    }

}
