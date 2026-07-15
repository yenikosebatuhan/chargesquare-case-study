package com.chargesquare.session.repo;

import com.chargesquare.session.domain.StopIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopIdempotencyRepository extends JpaRepository<StopIdempotency, String> {
}
