package com.jpd.web.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "video_watch_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoWatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id")
    private String userId;        // Keycloak sub ID

    @Column(name = "video_id")
    private String videoId;       // tham chiếu InspirationVideo.id

    @Column(name = "watched_at")
    private LocalDateTime watchedAt;

    @PrePersist
    protected void onCreate() {
        this.watchedAt = LocalDateTime.now();
    }
}
