package com.chargesquare.session.service;

import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.TariffSnapshot;
import com.chargesquare.session.domain.Wallet;
import com.chargesquare.session.error.ConflictException;
import com.chargesquare.session.error.NotFoundException;
import com.chargesquare.session.repo.SessionRepository;
import com.chargesquare.session.repo.WalletRepository;
import com.chargesquare.session.station.ConnectorView;
import com.chargesquare.session.station.StationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * The heart of the exercise: the guarded start/stop lifecycle plus wallet settlement.
 * The wallet is folded into this service (the recommended two-service default).
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessions;
    private final WalletRepository wallets;
    private final StationClient station;
    private final Clock clock;

    public SessionService(SessionRepository sessions, WalletRepository wallets,
                          StationClient station, Clock clock) {
        this.sessions = sessions;
        this.wallets = wallets;
        this.station = station;
        this.clock = clock;
    }

    /**
     * Start: validate + read the tariff from Station Service, occupy the connector,
     * then create an ACTIVE session with the tariff snapshotted. No session is created
     * if validation or occupy fails.
     */
    @Transactional
    public ChargingSession start(Long userId, Long connectorId) {
        ConnectorView connector = station.getConnector(connectorId);   // 404 -> CONNECTOR_NOT_FOUND
        if (!connector.isAvailable()) {
            throw new ConflictException("CONNECTOR_OCCUPIED",
                    "Connector " + connectorId + " is not AVAILABLE");
        }

        station.occupy(connectorId);   // authoritative flip; 409 -> CONNECTOR_OCCUPIED on a race

        ConnectorView.TariffView t = connector.tariff();
        TariffSnapshot snapshot = new TariffSnapshot(t.pricePerKwh(), t.startFee(), t.currency());
        ChargingSession session = sessions.save(
                ChargingSession.start(userId, connectorId, snapshot, Instant.now(clock)));

        log.info("session started {} (user {}, connector {})", session.getId(), userId, connectorId);
        return session;
    }

    /**
     * Stop: guard the state, price the energy from the snapshot, settle the wallet,
     * mark COMPLETED and free the connector. All in one transaction, so a failure to
     * reach Station Service on release rolls the whole stop back (fail-fast).
     */
    @Transactional
    public StopResult stop(Long sessionId, BigDecimal energyKwh) {
        ChargingSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND",
                        "Session " + sessionId + " does not exist"));

        if (!session.isActive()) {
            throw new ConflictException("SESSION_NOT_ACTIVE",
                    "Session " + sessionId + " is not ACTIVE");
        }

        BigDecimal cost = session.complete(energyKwh, Instant.now(clock));

        Wallet wallet = wallets.findById(session.getUserId())
                .orElseThrow(() -> new NotFoundException("WALLET_NOT_FOUND",
                        "No wallet for user " + session.getUserId()));
        wallet.debit(cost);
        log.info("wallet debited {} for user {} (balance now {})",
                cost, wallet.getUserId(), wallet.getBalance());

        station.release(session.getConnectorId());

        log.info("session stopped {}, charged {}", session.getId(), cost);
        return new StopResult(session, wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public SessionWithBalance getSession(Long sessionId) {
        ChargingSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND",
                        "Session " + sessionId + " does not exist"));
        return new SessionWithBalance(session, currentBalance(session.getUserId()));
    }

    @Transactional(readOnly = true)
    public List<SessionWithBalance> listUserSessions(Long userId) {
        BigDecimal balance = currentBalance(userId);
        return sessions.findByUserIdOrderByIdDesc(userId).stream()
                .map(s -> new SessionWithBalance(s, balance))
                .toList();
    }

    private BigDecimal currentBalance(Long userId) {
        return wallets.findById(userId).map(Wallet::getBalance).orElse(null);
    }

    /** Result of a stop: the completed session plus the wallet balance after settlement. */
    public record StopResult(ChargingSession session, BigDecimal walletBalanceAfter) {
    }

    public record SessionWithBalance(ChargingSession session, BigDecimal walletBalance) {
    }
}
