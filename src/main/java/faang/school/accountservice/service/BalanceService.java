package faang.school.accountservice.service;

import faang.school.accountservice.dto.BalanceDto;
import faang.school.accountservice.dto.TransactionDto;
import faang.school.accountservice.entity.Account;
import faang.school.accountservice.entity.Balance;
import faang.school.accountservice.exception.BalanceBelowZeroException;
import faang.school.accountservice.mapper.BalanceMapper;
import faang.school.accountservice.repository.BalanceRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceMapper balanceMapper;
    private final BalanceAuditService balanceAuditService;

    @Transactional
    public BalanceDto createBalance(Account account) {
        Balance balance = new Balance();
        balance.setAccount(account);
        balance.setActualBalance(BigDecimal.ZERO.setScale(2));
        balance.setAuthorizationBalance(BigDecimal.ZERO.setScale(2));
        balance = balanceRepository.save(balance);
        log.info("Balance for {} account is created", account.getAccountNumber());

        balanceAuditService.createAudit(balance, 1L);

        return balanceMapper.toDto(balance);
    }

    public BalanceDto getBalance(Long accountId) {
        Balance balance = getBalanceFromRepository(accountId);
        balanceAuditService.createAudit(balance, (long) balance.getVersion());
        log.info("Balance for {} account is found", accountId);
        return balanceMapper.toDto(balance);
    }

    @Transactional
    public BalanceDto updateBalance(Long accountId, TransactionDto transaction) {
        Balance balance = getBalanceFromRepository(accountId);

        BigDecimal amount = transaction.getAmount();
        BigDecimal actualBalance = balance.getActualBalance();
        BigDecimal authorizationBalance = balance.getAuthorizationBalance();

        switch (transaction.getOperationType()) {
            case CLEARING -> actualBalance = actualBalance.add(amount);
            case AUTHORIZATION -> authorizationBalance = authorizationBalance.add(amount);
        }

        if (authorizationBalance.compareTo(BigDecimal.ZERO) < 0 ||
                actualBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BalanceBelowZeroException(accountId, amount);
        }

        balance.setActualBalance(actualBalance);
        balance.setAuthorizationBalance(authorizationBalance);
        balance.setUpdatedAt(LocalDateTime.now());
        balance = balanceRepository.save(balance);

        balanceAuditService.createAudit(balance, transaction.getOperationId());

        log.info("Balance for {} account is updated", accountId);
        return balanceMapper.toDto(balance);
    }

    public void deleteBalance(long balanceId) {
        balanceRepository.deleteById(balanceId);
        log.info("Balance with id: {} deleted", balanceId);
    }

    private Balance getBalanceFromRepository(Long accountId) {
        return balanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found"));
    }
}
