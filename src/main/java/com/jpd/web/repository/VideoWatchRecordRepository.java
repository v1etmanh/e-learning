package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.VideoWatchRecord;

@Repository
public interface VideoWatchRecordRepository extends JpaRepository<VideoWatchRecord, String> {

    /**
     * Kiểm tra user đã xem video này chưa
     */
    boolean existsByUserIdAndVideoId(String userId, String videoId);

    /**
     * Lấy lịch sử xem của user
     */
    List<VideoWatchRecord> findByUserIdOrderByWatchedAtDesc(String userId);

    /**
     * Đếm số video user đã xem hôm nay (phục vụ streak)
     */
    long countByUserId(String userId);
}
