package com.chargesquare.session.web.dto;

import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.TariffSnapshot;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Session receipt. Null fields are omitted from JSON (see jackson.default-property-inclusion),
 * so a freshly started session shows its tariff snapshot while a completed one shows the cost.
 */
public record SessionResponse(
        Long sessionId,
        Long userId,
        Long connectorId,
        String status,
        Instant startedAt,
        Instant endedAt,
        BigDecimal energyKwh,
        BigDecimal cost,
        String currency,
        TariffSnapshotDto tariffSnapshot,
        BigDecimal walletBalanceAfter) {

    public record TariffSnapshotDto(BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
        static TariffSnapshotDto from(TariffSnapshot s) {
            return new TariffSnapshotDto(s.getPricePerKwh(), s.getStartFee(), s.getCurrency());
        }
    }

    /** Shape returned by POST /sessions (START): snapshot present, no cost yet. */
    public static SessionResponse started(ChargingSession s) {
        return new SessionResponse(
                s.getId(), s.getUserId(), s.getConnectorId(), s.getStatus().name(),
                s.getStartedAt(), null, null, null, null,
                TariffSnapshotDto.from(s.getTariffSnapshot()), null);
    }

    /** Shape returned by STOP and the read endpoints: full receipt plus wallet balance. */
    public static SessionResponse detail(ChargingSession s, BigDecimal walletBalance) {
        return new SessionResponse(
                s.getId(), s.getUserId(), s.getConnectorId(), s.getStatus().name(),
                s.getStartedAt(), s.getEndedAt(), s.getEnergyKwh(), s.getCost(),
                s.getTariffSnapshot().getCurrency(),
                TariffSnapshotDto.from(s.getTariffSnapshot()), walletBalance);
    }
}
