package com.cou.bustracker.service;

import com.cou.bustracker.event.NoticeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the writing transaction to the async push pipeline for notices.
 *
 * <p>Same split as {@link ScheduleChangedListener}: the listener is the
 * <em>synchronous</em> transaction hook, the dispatcher the <em>async</em>
 * worker. The hand-off crosses the proxy ({@code notificationDispatcher} is
 * injected), so {@code @Async} is honoured and a push can never be fired for
 * a write that later rolls back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeCreatedListener {

    private final NotificationDispatcher notificationDispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNoticeCreated(NoticeCreatedEvent event) {
        log.debug("Notice {} committed (audience {}) — handing off to push dispatcher",
                event.noticeId(), event.audience());
        notificationDispatcher.dispatch(event);
    }
}
