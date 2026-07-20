package com.jpd.web.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inspiration_video")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspirationVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String title;              // tiêu đề video

    @Column(columnDefinition = "TEXT")
    private String description;        // mô tả ngắn / lý do truyền cảm hứng

    private String speaker;            // tên diễn giả/nhân vật (nếu có)

    private String topic;              // chủ đề: motivation, career, life-lesson...

    @Column(name = "r2_object_key")
    private String r2ObjectKey;        // key file video trên R2 bucket

    @Column(name = "thumbnail_r2_key")
    private String thumbnailR2Key;     // key ảnh thumbnail trên R2

    @Column(columnDefinition = "LONGTEXT")
    private String transcript;         // transcript tiếng Anh đầy đủ

    private Integer durationSeconds;   // thời lượng video (giây)

    @Column(name = "difficulty_level")
    private String difficultyLevel;    // beginner / intermediate / advanced

    @Column(name = "assigned_date")
    private LocalDate assignedDate;    // ngày video này được chọn hiển thị

    @Column(name = "active")
    private Boolean active = false;    // true nếu đang thuộc batch 10 video của ngày hiện tại

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = false;
        }
    }
}
