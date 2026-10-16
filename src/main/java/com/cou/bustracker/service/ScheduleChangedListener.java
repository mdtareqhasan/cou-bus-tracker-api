package com.cou.bustracker.service;

import com.cou.bustracker.event.ScheduleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the writing transaction to the async push pipeline.
 *
 * <p>Split from {@link NotificationDispatcher} on purpose: the listener is the
 * <em>synchronous</em> transaction hook, the dispatcher is the <em>async</em>
 * worker. Keeping them on one bean makes {@code @TransactionalEventListener}
 * and {@code @Async} fight over the same method invocation, and risks firing a
 * push for a write that later rolls back. Here the hand-off crosses the proxy
 * ({@code notificationDispatcher} is injected), so {@code @Async} is honoured.
 *
 * <p>{@code AFTER_COMMIT} guarantees no notification leaves the server for a
 * transaction that never landed; {@code fallbackExecution} keeps the event
 * flowing even if something publishes outside a transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleChangedListener {

    private final NotificationDispatcher notificationDispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onScheduleChanged(ScheduleChangedEvent event) {
        log.debug("Schedule {} committed ({}) — handing off to push dispatcher",
                event.scheduleId(), event.action());
        notificationDispatcher.dispatch(event);
    }
}
