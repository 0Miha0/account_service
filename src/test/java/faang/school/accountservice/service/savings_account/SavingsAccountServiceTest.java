package faang.school.accountservice.service.savings_account;

import faang.school.accountservice.dto.savings_account.SavingsAccountCreateDto;
import faang.school.accountservice.dto.savings_account.SavingsAccountResponse;
import faang.school.accountservice.entity.Balance;
import faang.school.accountservice.entity.savings_account.SavingsAccount;
import faang.school.accountservice.entity.tariff.Tariff;
import faang.school.accountservice.enums.tariff.InterestPeriod;
import faang.school.accountservice.exception.UniqueConstraintException;
import faang.school.accountservice.mapper.savings_account.SavingsAccountMapper;
import faang.school.accountservice.repository.savings_account.SavingsAccountRepository;
import faang.school.accountservice.service.AccountService;
import faang.school.accountservice.service.BalanceAuditService;
import faang.school.accountservice.service.BalanceService;
import faang.school.accountservice.service.calculators.InterestCalculationHelper;
import faang.school.accountservice.service.tariff.TariffService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsAccountServiceTest {

    @Spy
    private SavingsAccountMapper savingsAccountMapper = Mappers.getMapper(SavingsAccountMapper.class);

    @Mock
    private AccountService accountService;

    @Mock
    private TariffService tariffService;

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private BalanceAuditService balanceAuditService;

    @Mock
    InterestCalculationHelper interestCalculationHelper;

    @InjectMocks
    private SavingsAccountService savingsAccountService;

    @Test
    void createSavingsAccountValidTest() {
        long baseAccountId = 1L;
        long tariffId = 2L;
        long savingsAccountId = 3L;
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);

        Account account = new Account();
        account.setId(baseAccountId);
        account.setType(AccountType.SAVINGS);
        Tariff tariff = new Tariff();
        tariff.setId(tariffId);
        tariff.setCurrentRate(tariffRate);
        SavingsAccountCreateDto savingsAccountCreateDto = SavingsAccountCreateDto.builder()
                .baseAccountId(baseAccountId)
                .tariffId(tariffId)
                .build();
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setId(savingsAccountId);
        savingsAccount.changeTariff(tariff);
        savingsAccount.setAccount(account);

        when(tariffService.getTariffById(tariffId)).thenReturn(tariff);
        when(accountService.getAccountById(baseAccountId)).thenReturn(account);
        when(savingsAccountRepository.save(any())).thenReturn(savingsAccount);

        SavingsAccountResponse response = assertDoesNotThrow(() -> savingsAccountService.createSavingsAccount(savingsAccountCreateDto));

        assertEquals(savingsAccountId, response.getId());
        assertEquals(baseAccountId, response.getBaseAccountId());
        assertEquals(tariffId, response.getCurrentTariffId());
        assertEquals(tariffRate, response.getCurrentRate());
        verify(accountService, times(1)).getAccountById(baseAccountId);
        verify(tariffService, times(1)).getTariffById(tariffId);
        verify(savingsAccountRepository, times(1)).save(any());
    }

    @Test
    void createSavingsAccountAlreadyExistingBaseAccountTest() {
        long baseAccountId = 1L;
        long tariffId = 2L;

        Account account = new Account();
        account.setId(baseAccountId);
        account.setType(AccountType.SAVINGS);
        SavingsAccountCreateDto savingsAccountCreateDto = SavingsAccountCreateDto.builder()
                .baseAccountId(baseAccountId)
                .tariffId(tariffId)
                .build();

        when(accountService.getAccountById(baseAccountId)).thenReturn(account);
        when(savingsAccountRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("constraint [savings_account_account_id_key]"));

        assertThrows(UniqueConstraintException.class, () -> savingsAccountService.createSavingsAccount(savingsAccountCreateDto));
    }

    @Test
    void createSavingsAccountBasedOnNotSavingsTypeAccountTest() {
        long baseAccountId = 1L;
        long tariffId = 2L;

        Account account = new Account();
        account.setId(baseAccountId);
        account.setType(AccountType.DEBIT);
        SavingsAccountCreateDto savingsAccountCreateDto = SavingsAccountCreateDto.builder()
                .baseAccountId(baseAccountId)
                .tariffId(tariffId)
                .build();

        when(accountService.getAccountById(baseAccountId)).thenReturn(account);

        assertThrows(IllegalArgumentException.class, () -> savingsAccountService.createSavingsAccount(savingsAccountCreateDto));
    }

    @Test
    void updateSavingsAccountTariffValidTest() {
        long savingsAccountId = 1L;
        long tariffId = 2L;
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);

        Tariff tariff = new Tariff();
        tariff.setId(tariffId);
        tariff.setCurrentRate(tariffRate);
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setId(savingsAccountId);
        savingsAccount.changeTariff(tariff);

        when(tariffService.getTariffById(tariffId)).thenReturn(tariff);
        when(savingsAccountRepository.findById(savingsAccountId)).thenReturn(Optional.of(new SavingsAccount()));
        when(savingsAccountRepository.save(any())).thenReturn(savingsAccount);

        SavingsAccountResponse response = assertDoesNotThrow(() ->
                savingsAccountService.updateSavingsAccountTariff(savingsAccountId, tariffId));

        assertEquals(savingsAccountId, response.getId());
        assertEquals(tariffId, response.getCurrentTariffId());
        assertEquals(tariffRate, response.getCurrentRate());
        verify(tariffService, times(1)).getTariffById(tariffId);
        verify(savingsAccountRepository, times(1)).save(any());
    }

    @Test
    void updateSavingsAccountNotFoundTest() {
        long savingsAccountId = 1L;
        long tariffId = 2L;
        when(savingsAccountRepository.findById(savingsAccountId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> savingsAccountService.updateSavingsAccountTariff(savingsAccountId, tariffId));
    }

    @Test
    void getSavingsAccountByIdValidTest() {
        long baseAccountId = 1L;
        long tariffId = 2L;
        long savingsAccountId = 3L;
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);

        Account account = new Account();
        account.setId(baseAccountId);
        Tariff tariff = new Tariff();
        tariff.setId(tariffId);
        tariff.setCurrentRate(tariffRate);
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setId(savingsAccountId);
        savingsAccount.changeTariff(tariff);
        savingsAccount.setAccount(account);

        when(savingsAccountRepository.findById(savingsAccountId)).thenReturn(Optional.of(savingsAccount));

        SavingsAccountResponse response = assertDoesNotThrow(() -> savingsAccountService.getSavingsAccountById(savingsAccountId));

        assertEquals(savingsAccountId, response.getId());
        assertEquals(baseAccountId, response.getBaseAccountId());
        assertEquals(tariffId, response.getCurrentTariffId());
        assertEquals(tariffRate, response.getCurrentRate());
        verify(savingsAccountRepository, times(1)).findById(savingsAccountId);
    }

    @Test
    void getSavingsAccountByIdNotFoundTest() {
        long savingsAccountId = 3L;
        when(savingsAccountRepository.findById(savingsAccountId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> savingsAccountService.getSavingsAccountById(savingsAccountId));
    }

    @Test
    void getSavingsAccountByOwnerIdValidTest() {
        long baseAccountId = 1L;
        long tariffId = 2L;
        long savingsAccountId = 3L;
        long ownerId = 4L;
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);

        Account account = new Account();
        account.setId(baseAccountId);
        Tariff tariff = new Tariff();
        tariff.setId(tariffId);
        tariff.setCurrentRate(tariffRate);
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setId(savingsAccountId);
        savingsAccount.changeTariff(tariff);
        savingsAccount.setAccount(account);

        when(savingsAccountRepository.getSavingsAccountsByOwnerId(ownerId)).thenReturn(List.of(savingsAccount));

        List<SavingsAccountResponse> responses = assertDoesNotThrow(() -> savingsAccountService.getSavingsAccountsByOwnerId(ownerId));

        assertEquals(savingsAccountId, responses.get(0).getId());
        assertEquals(baseAccountId, responses.get(0).getBaseAccountId());
        assertEquals(tariffId, responses.get(0).getCurrentTariffId());
        assertEquals(tariffRate, responses.get(0).getCurrentRate());
        verify(savingsAccountRepository, times(1)).getSavingsAccountsByOwnerId(ownerId);
    }

    @Test
    void accrueInterestNotAccrualDayTest() {
        Tariff tariff = Tariff.builder()
                .interestPeriod(InterestPeriod.WEEKLY)
                .currentRate(BigDecimal.valueOf(15))
                .build();
        SavingsAccount savingsAccount = SavingsAccount.builder()
                .id(1L)
                .lastInterestDate(LocalDateTime.now().minusDays(2))
                .tariff(tariff)
                .build();
        when(interestCalculationHelper.getInterestStartDate(any(), any())).thenReturn(LocalDateTime.now().minusWeeks(1));

        assertDoesNotThrow(() -> savingsAccountService.accrueInterest(savingsAccount));

        verify(interestCalculationHelper, times(1)).getInterestStartDate(any(), any());
    }

    @Test
    void accrueInterestAccrualDayWithoutBalanceAuditsTest() {
        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        long accountId = 1L;
        BigDecimal actualBalance = new BigDecimal(15000);

        Tariff tariff = Tariff.builder()
                .interestPeriod(InterestPeriod.WEEKLY)
                .currentRate(BigDecimal.valueOf(15))
                .build();
        Balance balance = new Balance();
        balance.setActualBalance(actualBalance);
        Account account = Account.builder()
                .id(1L)
                .balance(balance)
                .build();
        SavingsAccount savingsAccount = SavingsAccount.builder()
                .id(1L)
                .account(account)
                .lastInterestDate(LocalDateTime.now().minusWeeks(2))
                .tariff(tariff)
                .build();
        when(interestCalculationHelper.getInterestStartDate(any(), any())).thenReturn(LocalDateTime.now().minusWeeks(1));
        when(balanceAuditService.findMinimalActualBalanceByAccountAndPeriod(eq(accountId), any(), any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> savingsAccountService.accrueInterest(savingsAccount));

        verify(interestCalculationHelper, times(1)).getInterestStartDate(any(), any());
        verify(balanceAuditService, times(1)).findMinimalActualBalanceByAccountAndPeriod(eq(accountId), any(), any());
        verify(interestCalculationHelper, times(1)).calculateBalanceInterest(balanceCaptor.capture(), eq(tariff));
        verify(balanceService, times(1)).updateBalance(eq(accountId), any());
        verify(savingsAccountRepository, times(1)).save(any());

        assertEquals(actualBalance, balanceCaptor.getValue());
    }

    @Test
    void accrueInterestAccrualDayWithBalanceAuditsTest() {
        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        long accountId = 1L;
        BigDecimal actualBalance = new BigDecimal(15000);
        BigDecimal minimalBalanceAuditBalance = new BigDecimal(10000);

        Tariff tariff = Tariff.builder()
                .interestPeriod(InterestPeriod.WEEKLY)
                .currentRate(BigDecimal.valueOf(15))
                .build();
        Balance balance = new Balance();
        balance.setActualBalance(actualBalance);
        Account account = Account.builder()
                .id(1L)
                .balance(balance)
                .build();
        SavingsAccount savingsAccount = SavingsAccount.builder()
                .id(1L)
                .account(account)
                .lastInterestDate(LocalDateTime.now().minusWeeks(2))
                .tariff(tariff)
                .build();
        when(interestCalculationHelper.getInterestStartDate(any(), any())).thenReturn(LocalDateTime.now().minusWeeks(1));
        when(balanceAuditService.findMinimalActualBalanceByAccountAndPeriod(eq(accountId), any(), any()))
                .thenReturn(Optional.of(minimalBalanceAuditBalance));

        assertDoesNotThrow(() -> savingsAccountService.accrueInterest(savingsAccount));

        verify(interestCalculationHelper, times(1)).getInterestStartDate(any(), any());
        verify(balanceAuditService, times(1)).findMinimalActualBalanceByAccountAndPeriod(eq(accountId), any(), any());
        verify(interestCalculationHelper, times(1)).calculateBalanceInterest(balanceCaptor.capture(), eq(tariff));
        verify(balanceService, times(1)).updateBalance(eq(accountId), any());
        verify(savingsAccountRepository, times(1)).save(any());

        assertEquals(minimalBalanceAuditBalance, balanceCaptor.getValue());
    }
}