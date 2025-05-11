package faang.school.accountservice.service;

import faang.school.accountservice.dto.BalanceDto;
import faang.school.accountservice.dto.TransactionDto;
import faang.school.accountservice.entity.Balance;
import faang.school.accountservice.enums.OperationType;
import faang.school.accountservice.exception.BalanceBelowZeroException;
import faang.school.accountservice.mapper.BalanceMapper;
import faang.school.accountservice.repository.BalanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceAuditService balanceAuditService;

    @Spy
    private BalanceMapper balanceMapper = Mappers.getMapper(BalanceMapper.class);

    @InjectMocks
    private BalanceService balanceService;

    @Test
    public void createBalanceTest() {
        Long accountId = 1L;
        Long balanceId = 1L;
        Account account = new Account();
        account.setId(accountId);
        account.setAccountNumber("123");

        Balance savedBalance = new Balance();
        savedBalance.setId(balanceId);
        savedBalance.setActualBalance(BigDecimal.ZERO);
        savedBalance.setAuthorizationBalance(BigDecimal.ZERO);
        savedBalance.setAccount(account);

        when(balanceRepository.save(any(Balance.class))).thenReturn(savedBalance);

        BalanceDto balanceDto = balanceService.createBalance(account);

        verify(balanceRepository, times(1)).save(any(Balance.class));

        assertNotNull(balanceDto);
        assertEquals(BigDecimal.ZERO, balanceDto.getActualBalance());
        assertEquals(BigDecimal.ZERO, balanceDto.getAuthorizationBalance());
        assertEquals(account.getAccountNumber(), balanceDto.getAccount().getAccountNumber());
    }

    @Test
    public void getNonExistentBalanceTest() {
        Long accountId = 1L;
        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> balanceService.getBalance(accountId));
    }

    @Test
    public void getBalanceTest() {
        Long accountId = 1L;
        Balance balance = new Balance();
        balance.setId(2L);
        BalanceDto expectedBalanceDto = balanceMapper.toDto(balance);
        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));

        BalanceDto receivedBalance  = balanceService.getBalance(accountId);

        assertEquals(expectedBalanceDto.getId(), receivedBalance.getId());
    }

    @Test
    public void updateBalanceToAmountBelowZeroOperationAuthorizationTest() {
        Long accountId = 1L;
        Long operationId = 1L;
        Balance balance = new Balance();
        balance.setId(2L);
        balance.setAuthorizationBalance(BigDecimal.valueOf(50));
        balance.setActualBalance(BigDecimal.valueOf(50));
        TransactionDto transactionDto = new TransactionDto(operationId, BigDecimal.valueOf(-100), OperationType.AUTHORIZATION);
        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));

        assertThrows(BalanceBelowZeroException.class, () -> balanceService.updateBalance(accountId, transactionDto));
    }

    @Test
    public void updateBalanceToAmountBelowZeroOperationClearingTest() {
        Long accountId = 1L;
        Long operationId = 1L;
        Balance balance = new Balance();
        balance.setId(2L);
        balance.setAuthorizationBalance(BigDecimal.valueOf(50));
        balance.setActualBalance(BigDecimal.valueOf(50));
        TransactionDto transactionDto = new TransactionDto(operationId, BigDecimal.valueOf(-100), OperationType.CLEARING);
        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));

        assertThrows(BalanceBelowZeroException.class, () -> balanceService.updateBalance(accountId, transactionDto));
    }

    @Test
    public void updateBalanceTest() {
        Long entityId = 1L;

        Account account = new Account();
        account.setId(entityId);
        account.setAccountNumber("123");

        Balance balance = new Balance();
        balance.setAccount(account);
        balance.setId(entityId);
        balance.setAuthorizationBalance(BigDecimal.valueOf(50));
        balance.setActualBalance(BigDecimal.valueOf(50));

        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAmount(BigDecimal.valueOf(-10));
        transactionDto.setOperationType(OperationType.AUTHORIZATION);
        transactionDto.setOperationId(entityId);

        when(balanceRepository.findByAccountId(entityId)).thenReturn(Optional.of(balance));
        when(balanceRepository.save(any(Balance.class))).thenReturn(balance);

        BalanceDto actualBalanceDto = balanceService.updateBalance(entityId, transactionDto);

        assertNotNull(actualBalanceDto);
        assertEquals(BigDecimal.valueOf(40), actualBalanceDto.getAuthorizationBalance());
        assertEquals(BigDecimal.valueOf(50), actualBalanceDto.getActualBalance());

        verify(balanceRepository, times(1)).findByAccountId(entityId);
        verify(balanceRepository, times(1)).save(any(Balance.class));
    }
}
