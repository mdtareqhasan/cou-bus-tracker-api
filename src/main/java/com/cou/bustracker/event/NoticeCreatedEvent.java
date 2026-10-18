package com.cou.bustracker.event;

import com.cou.bustracker.entity.Notice;

import java.time.LocalDateTime;

/**
 * Immutable snapshot of a freshly created notice, published after the DB commit.
 *
 * <p>Everything a push needs is copied out of the entity <em>while the
 * transaction is still open</em> — the AFTER_COMMIT listener must never touch
 * the entity itself. A plain record also means the payload can cross to the
 * {@code notificationExecutor} with no session, no proxy and no lazy loading.
 *
 * <p>Unlike {@link ScheduleChangedEvent}, the text is carried as-is instead of
 * as structured diffs: a notice <em>is</em> admin-authored prose, the exact
 * words riders should read, so there is nothing to localize server-side.
 *
 * @param noticeId  id of the notice
 * @param title     admin-authored title, pushed verbatim
 * @param body      admin-authored body, pushed verbatim (truncated at push time)
 * @param audience  STUDENT / TEACHER / ALL — decides which token groups are targeted
 * @param expiresAt when the notice stops being active, may be null
 */
public record NoticeCreatedEvent(
        Long noticeId,
        String title,
        String body,
        String audience,
        LocalDateTime expiresAt) {

    /**
     * Snapshots a live entity. <strong>Must be called inside the transaction</strong>
     * that owns {@code notice} — never from an after-commit listener.
     */
    public static NoticeCreatedEvent from(Notice notice) {
        return new NoticeCreatedEvent(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getAudience(),
                notice.getExpiresAt());
    }
}
