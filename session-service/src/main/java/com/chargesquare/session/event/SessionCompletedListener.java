package com.chargesquare.session.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Stub subscriber for the SessionCompleted event. Fires only after the stop transaction commits,
 * so it never observes a rolled-back session. Here it just logs; a real consumer would publish
 * to a broker or feed analytics.
 */
@Component
public class SessionCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(SessionCompletedListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SessionCompleted event) {
        log.info("event SessionCompleted session={} user={} connector={} energyKwh={} cost={} at={}",
                event.sessionId(), event.userId(), event.connectorId(),
                event.energyKwh(), event.cost(), event.occurredAt());
    }
}
