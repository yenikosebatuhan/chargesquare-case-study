package com.chargesquare.wallet.repo;

import com.chargesquare.wallet.domain.ProcessedDebit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedDebitRepository extends JpaRepository<ProcessedDebit, String> {
}
