
package com.klh.pettycashmanager;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CashRecordRepository
        extends JpaRepository<CashRecord, Long> {
}
