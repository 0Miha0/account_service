package faang.school.accountservice.service;

import faang.school.accountservice.entity.Balance;
import faang.school.accountservice.entity.BalanceAudit;
import faang.school.accountservice.repository.BalanceAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceAuditService {

    private final BalanceAuditRepository balanceAuditRepository;

    public void createAudit(Balance balance, Long operationId) {
        BalanceAudit audit = BalanceAudit.builder()
                .account(balance.getAccount())
                .balanceVersion((long) balance.getVersion())
                .authorizationBalance(balance.getAuthorizationBalance())
                .actualBalance(balance.getActualBalance())
                .operationId(operationId)
                .createdAt(LocalDateTime.now())
                .build();
        balanceAuditRepository.save(audit);
        log.info("Balance audit for account {} with version {} is created",
                balance.getAccount().getId(), balance.getVersion());
    }

    public Optional<BigDecimal> findMinimalActualBalanceByAccountAndPeriod(
            long accountId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return balanceAuditRepository.findMinimalActualBalanceByAccountAndPeriod(accountId, startDate, endDate);
    }

    public void deleteAudit(long auditId) {
        balanceAuditRepository.deleteById(auditId);
        log.info("Balance audit with Id: {} deleted",auditId);
    }
}
