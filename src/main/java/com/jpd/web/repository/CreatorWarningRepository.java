package com.jpd.web.repository;

import com.jpd.web.model.Creator;
import com.jpd.web.model.CreatorWarning;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreatorWarningRepository extends JpaRepository<CreatorWarning, Long> {

    List<CreatorWarning> findByCreatorAndIsActiveTrue(Creator creator);

    @Query("SELECT COUNT(cw) FROM CreatorWarning cw " +
            "WHERE cw.creator = :creator AND cw.isActive = true " +
            "AND cw.issuedAt >= :since")
    Long countByCreatorAndIssuedAtAfter(
            @Param("creator") Creator creator,
            @Param("since") LocalDateTime since);

    List<CreatorWarning> findByCreatorOrderByIssuedAtDesc(Creator creator);

    Long countByIsActive(boolean isActive);

    Long countByCreator_CreatorId(String creatorId);

    @Query("SELECT COUNT(cw) FROM CreatorWarning cw WHERE cw.creator.creatorId = :creatorId AND cw.isActive = true")
    Long countActiveByCreatorId(@Param("creatorId") String creatorId);

    // Find queries
    List<CreatorWarning> findByCreator_CreatorId(String creatorId);

    List<CreatorWarning> findByIsActive(boolean isActive);

    List<CreatorWarning> findByIssuedByAdmin(String adminId);

    List<CreatorWarning> findTopByOrderByIssuedAtDesc(Pageable pageable);

    @Query("SELECT cw FROM CreatorWarning cw LEFT JOIN FETCH cw.creator WHERE cw.warningId = :warningId")
    CreatorWarning findByIdWithCreator(@Param("warningId") Long warningId);

    @Query("SELECT cw FROM CreatorWarning cw WHERE cw.creator.creatorId = :creatorId AND cw.isActive = true")
    List<CreatorWarning> findActiveWarningsByCreatorId(@Param("creatorId") String creatorId);

    @Query("SELECT cw FROM CreatorWarning cw WHERE cw.issuedAt >= :date")
    List<CreatorWarning> findWarningsIssuedAfter(@Param("date") LocalDateTime date);
}
