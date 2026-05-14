package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "writing_result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;

    private Double grammar;

    private Double vocabulary;

    @Column(columnDefinition = "TEXT")
    private String feedback;
    @CreationTimestamp
    private LocalDateTime createDate;
    private long contentId;
}
