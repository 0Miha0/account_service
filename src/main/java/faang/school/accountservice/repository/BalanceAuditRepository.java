package faang.school.accountservice.repository;

import faang.school.accountservice.entity.BalanceAudit;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceAuditRepository extends JpaRepository<BalanceAudit, Long> {

    List<BalanceAudit> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Query(value = """
            SELECT MIN(balance_audit.actual_balance) 
            FROM balance_audit 
            JOIN account 
            ON balance_audit.account_id = account.id 
            WHERE account.id = :accountId 
            AND balance_audit.created_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    Optional<BigDecimal> findMinimalActualBalanceByAccountAndPeriod(
            @Param("accountId") long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
