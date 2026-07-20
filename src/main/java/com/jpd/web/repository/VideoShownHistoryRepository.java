package com.jpd.web.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.VideoShownHistory;

@Repository
public interface VideoShownHistoryRepository extends JpaRepository<VideoShownHistory, String> {

    /**
     * Lấy list videoId đã hiển thị từ ngày X trở đi
     */
    @Query("SELECT h.videoId FROM VideoShownHistory h WHERE h.shownDate >= :since")
    List<String> findVideoIdsShownSince(@Param("since") LocalDate since);

    /**
     * Xóa history cũ hơn N ngày (dọn dẹp nếu cần)
     */
    void deleteByShownDateBefore(LocalDate date);
}
