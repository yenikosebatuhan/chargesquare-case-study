package com.chargesquare.session.service;

import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.TariffSnapshot;
import com.chargesquare.session.error.ConflictException;
import com.chargesquare.session.error.NotFoundException;
import com.chargesquare.session.domain.SessionStatus;
import com.chargesquare.session.domain.StopIdempotency;
import com.chargesquare.session.event.SessionCompleted;
import com.chargesquare.session.repo.SessionRepository;
import com.chargesquare.session.repo.StopIdempotencyRepository;
import com.chargesquare.session.station.ConnectorView;
import com.chargesquare.session.station.StationClient;
import com.chargesquare.session.wallet.WalletClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * The heart of the exercise: the guarded start/stop lifecycle. Settlement is delegated to the
 * Wallet Service over REST with an idempotent debit keyed by the session id, so a retried stop
 * (which may re-issue the debit or re-run after a failed release) never double-charges.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessions;
    private final StopIdempotencyRepository stopKeys;
    private final StationClient station;
    private final WalletClient wallet;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SessionService(SessionRepository sessions, StopIdempotencyRepository stopKeys,
                          StationClient station, WalletClient wallet,
                          ApplicationEventPublisher events, Clock clock) {
        this.sessions = sessions;
        this.stopKeys = stopKeys;
        this.station = station;
        this.wallet = wallet;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Start: validate + read the tariff from Station Service, occupy the connector (honouring a
     * reservation the same user holds), then create an ACTIVE session with the tariff snapshotted.
     * No session is created if validation or occupy fails.
     */
    @Transactional
    public ChargingSession start(Long userId, Long connectorId) {
        ConnectorView connector = station.getConnector(connectorId);   // 404 -> CONNECTOR_NOT_FOUND
        if (!connector.isStartable(userId)) {
            throw new ConflictException("CONNECTOR_OCCUPIED",
                    "Connector " + connectorId + " is not available to this user");
        }

        station.occupy(connectorId, userId);   // authoritative flip; 409 on a race / foreign reservation

        ConnectorView.TariffView t = connector.tariff();
        TariffSnapshot snapshot = new TariffSnapshot(t.pricePerKwh(), t.startFee(), t.currency());
        ChargingSession session = sessions.save(
                ChargingSession.start(userId, connectorId, snapshot, Instant.now(clock)));

        log.info("session started {} (user {}, connector {})", session.getId(), userId, connectorId);
        return session;
    }

    /**
     * Stop: guard the state, price the energy from the snapshot, settle the wallet (idempotent
     * remote debit), mark COMPLETED and free the connector. One transaction: if the debit or the
     * release fails the stop rolls back, and a retry is safe because the debit is idempotent.
     */
    @Transactional
    public ChargingSession stop(Long sessionId, BigDecimal energyKwh, String idempotencyKey) {
        // Client-facing idempotency: a repeat with the same key replays the original receipt.
        if (idempotencyKey != null) {
            StopIdempotency prior = stopKeys.findById(idempotencyKey).orElse(null);
            if (prior != null) {
                if (!prior.getSessionId().equals(sessionId)) {
                    throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                            "Idempotency key already used for a different session");
                }
                log.info("stop replay for key {} — returning session {}", idempotencyKey, sessionId);
                return getSession(sessionId);
            }
        }

        ChargingSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND",
                        "Session " + sessionId + " does not exist"));

        if (!session.isActive()) {
            throw new ConflictException("SESSION_NOT_ACTIVE",
                    "Session " + sessionId + " is not ACTIVE");
        }

        BigDecimal cost = session.complete(energyKwh, Instant.now(clock));

        // Settle via the Wallet Service. The debit is idempotent (keyed by session id), so a
        // retry after a failed release re-issues it safely without double-charging.
        WalletClient.DebitResult debit = wallet.debit(session.getUserId(), cost, debitKey(sessionId));
        session.recordSettlement(debit.balance());

        station.release(session.getConnectorId());

        if (idempotencyKey != null) {
            stopKeys.save(new StopIdempotency(idempotencyKey, sessionId, Instant.now(clock)));
        }

        events.publishEvent(new SessionCompleted(
                session.getId(), session.getUserId(), session.getConnectorId(),
                energyKwh, cost, Instant.now(clock)));

        log.info("session stopped {}, charged {} (wallet now {})", session.getId(), cost, debit.balance());
        return session;
    }

    private static String debitKey(Long sessionId) {
        return "session-" + sessionId;
    }

    /** Reserve a connector for a user (orchestrates the Station reservation). */
    public ConnectorView reserve(Long userId, Long connectorId, Integer ttlSeconds) {
        ConnectorView reserved = station.reserve(connectorId, userId, ttlSeconds);
        log.info("connector {} reserved for user {}", connectorId, userId);
        return reserved;
    }

    @Transactional(readOnly = true)
    public ChargingSession getSession(Long sessionId) {
        return sessions.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND",
                        "Session " + sessionId + " does not exist"));
    }

    @Transactional(readOnly = true)
    public List<ChargingSession> listUserSessions(Long userId) {
        return sessions.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSessionOnConnector(Long connectorId) {
        return sessions.existsByConnectorIdAndStatus(connectorId, SessionStatus.ACTIVE);
    }
}
