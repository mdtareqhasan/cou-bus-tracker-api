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

    /**
     * The rider-facing query behind {@code /api/notices/active}: active,
     * non-expired notices whose audience is in the caller's allowed set,
     * newest first.
     *
     * <p>Ordering matters — the old un-ordered query let a three-day-old
     * notice outrank one posted five minutes ago, because Postgres returns
     * rows in physical order when no {@code ORDER BY} is given.
     *
     * @param now        request time; expiry is evaluated server-side
     * @param audiences e.g. {@code [ALL, STUDENT]} for a student
     */
    @Query("""
            SELECT n FROM Notice n
            WHERE n.isActive = true
              AND (n.expiresAt IS NULL OR n.expiresAt > :now)
              AND n.audience IN :audiences
            ORDER BY n.createdAt DESC
            """)
    List<Notice> findActiveNoticesForAudiences(LocalDateTime now, List<String> audiences);

    List<Notice> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Notice> findAllByOrderByCreatedAtDesc();
}
