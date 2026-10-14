package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n WHERE n.isActive = true AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    List<Notice> findActiveNotices(LocalDateTime now);

    List<Notice> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Notice> findAllByOrderByCreatedAtDesc();
}
