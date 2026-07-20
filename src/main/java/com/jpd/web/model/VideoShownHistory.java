package com.jpd.web.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "video_shown_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoShownHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "video_id")
    private String videoId;       // tham chiếu InspirationVideo.id

    @Column(name = "shown_date")
    private LocalDate shownDate;  // ngày đã hiển thị
}
