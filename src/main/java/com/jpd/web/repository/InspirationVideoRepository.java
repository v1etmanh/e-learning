package com.jpd.web.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jpd.web.model.InspirationVideo;

@Repository
public interface InspirationVideoRepository extends JpaRepository<InspirationVideo, String> {

    /**
     * Lấy danh sách video đang active (batch hiện tại)
     */
    List<InspirationVideo> findByActiveTrueOrderByAssignedDateDesc();

    /**
     * Lấy video candidate: chưa hiển thị trong N ngày gần nhất
     * (loại trừ video đã nằm trong bảng video_shown_history từ cutoffDate trở đi)
     */
    @Query("SELECT v FROM InspirationVideo v WHERE v.id NOT IN " +
           "(SELECT h.videoId FROM VideoShownHistory h WHERE h.shownDate >= :cutoffDate)")
    List<InspirationVideo> findCandidateVideos(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Deactivate toàn bộ video (chuẩn bị cho batch mới)
     */
    @Transactional
    @Modifying
    @Query("UPDATE InspirationVideo v SET v.active = false WHERE v.active = true")
    void deactivateAll();

    /**
     * Kiểm tra video đã tồn tại theo r2ObjectKey (tránh duplicate khi sync từ R2)
     */
    boolean existsByR2ObjectKey(String r2ObjectKey);

    /**
     * Đếm tổng số video trong ngân hàng
     */
    long count();
}
