package com.chargesquare.session.repo;

import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<ChargingSession, Long> {
    List<ChargingSession> findByUserIdOrderByIdDesc(Long userId);

    boolean existsByConnectorIdAndStatus(Long connectorId, SessionStatus status);
}
