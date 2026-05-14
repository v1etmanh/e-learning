package com.jpd.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class CourseMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long courseDescriptionId;

    // Statistics
    private String instructorName;

    @Min(0)
    private int totalStudents;

    @Min(0)
    private int totalFeedbacks;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private double averageRating;

    @Min(0)
    private int totalRating;



    @Min(0)
    private int totalPeopleLike;

    @Min(0)
    private int totalReport;

    // Optional extended metrics
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double completionRate; // 0.0 - 1.0



    private Integer durationMinutes; // total duration

    private String categories; // comma-separated categories (simplified)

    private String tags; // comma-separated tags (simplified)

    // Audit timestamps
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastEnrollmentAt;
    private LocalDateTime lastRatingAt;
    private LocalDateTime lastLikeAt;
    private LocalDateTime lastReportAt;

    @OneToOne
    @JoinColumn(name = "course_id")
    private Course course;
}
