package faang.school.accountservice.service.savings_account;

import faang.school.accountservice.dto.TransactionDto;
import faang.school.accountservice.dto.savings_account.SavingsAccountCreateDto;
import faang.school.accountservice.dto.savings_account.SavingsAccountResponse;
import faang.school.accountservice.entity.Account;
import faang.school.accountservice.entity.savings_account.SavingsAccount;
import faang.school.accountservice.entity.tariff.Tariff;
import faang.school.accountservice.enums.AccountType;
import faang.school.accountservice.enums.OperationType;
import faang.school.accountservice.exception.UniqueConstraintException;
import faang.school.accountservice.mapper.savings_account.SavingsAccountMapper;
import faang.school.accountservice.repository.savings_account.SavingsAccountRepository;
import faang.school.accountservice.service.AccountService;
import faang.school.accountservice.service.BalanceAuditService;
import faang.school.accountservice.service.BalanceService;
import faang.school.accountservice.service.calculators.InterestCalculationHelper;
import faang.school.accountservice.service.tariff.TariffService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleStateException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsAccountService {

    private static final String ACCOUNT_UNIQUE_CONSTRAINT = "savings_account_account_id_key";

    private final SavingsAccountMapper savingsAccountMapper;
    private final AccountService accountService;
    private final TariffService tariffService;
    private final SavingsAccountRepository savingsAccountRepository;
    private final BalanceService balanceService;
    private final BalanceAuditService balanceAuditService;
    private final InterestCalculationHelper interestCalculationHelper;

    @Transactional
    public SavingsAccountResponse createSavingsAccount(SavingsAccountCreateDto creationDto) {
        log.info("Received request to create SavingAccount based on account (ID={}) and tariff (ID={})",
                creationDto.getBaseAccountId(), creationDto.getTariffId());

        SavingsAccountResponse response = null;
        try {
            Account account = accountService.getAccountById(creationDto.getBaseAccountId());
            validateAccountType(account);
            Tariff tariff = tariffService.getTariffById(creationDto.getTariffId());
            SavingsAccount savingsAccount = new SavingsAccount();
            savingsAccount.changeTariff(tariff);
            savingsAccount.setAccount(account);
            savingsAccount = savingsAccountRepository.save(savingsAccount);

            response = savingsAccountMapper.toResponse(savingsAccount);
            log.info("New savings account based on account (ID={}) and tariff (ID={}) was created",
                    creationDto.getBaseAccountId(), creationDto.getTariffId());
        } catch (DataIntegrityViolationException ex) {
            handleDataIntegrityViolationException(ex, creationDto.getBaseAccountId());
        }
        return response;
    }

    @Retryable(
            retryFor = OptimisticLockException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Transactional
    public SavingsAccountResponse updateSavingsAccountTariff(long savingsAccountId, long tariffId) {
        log.info("Received request to set new tariff with ID={} for savings account with ID={}", tariffId, savingsAccountId);
        Tariff tariff = tariffService.getTariffById(tariffId);
        SavingsAccount savingsAccount = getSavingsAccount(savingsAccountId);
        savingsAccount.changeTariff(tariff);
        savingsAccount = savingsAccountRepository.save(savingsAccount);
        log.info("New tariff with ID={} was settled for savings account with ID={}", tariffId, savingsAccountId);
        return savingsAccountMapper.toResponse(savingsAccount);
    }

    @Recover
    public void recover(OptimisticLockException ex, long savingsAccountId, long tariffId) {
        log.error("Retries exhausted. Could not set new tariff with ID={} for savings account with ID={}. Exception: {}",
                tariffId, savingsAccountId, ex.getMessage(), ex);
    }

    @Transactional(readOnly = true)
    public SavingsAccountResponse getSavingsAccountById(long savingsAccountId) {
        log.info("Received request to find savings account with ID={}", savingsAccountId);
        SavingsAccount savingsAccount = getSavingsAccount(savingsAccountId);
        log.info("Savings account with ID={} was successfully found", savingsAccountId);
        return savingsAccountMapper.toResponse(savingsAccount);
    }

    @Transactional(readOnly = true)
    public List<SavingsAccountResponse> getSavingsAccountsByOwnerId(long accountOwnerId) {
        log.info("Received request to find savings account by owner ID={}", accountOwnerId);
        List<SavingsAccount> savingsAccounts = savingsAccountRepository.getSavingsAccountsByOwnerId(accountOwnerId);
        log.info("{} savings account were found for owner with ID={}", savingsAccounts.size(), accountOwnerId);
        return savingsAccountMapper.toResponseList(savingsAccounts);
    }

    @Transactional
    public void accrueInterest(SavingsAccount savingsAccount) {
        try {
            log.info("Accruing interest for Savings account with ID={}", savingsAccount.getId());

            LocalDateTime interestEndDate = LocalDate.now().atStartOfDay();
            LocalDateTime interestStartDate = interestCalculationHelper.getInterestStartDate(
                    interestEndDate, savingsAccount.getTariff().getInterestPeriod());

            if (canAccrueInterest(savingsAccount, interestStartDate)) {
                BigDecimal minimalActualBalanceForPeriod = getMinimalActualBalanceForPeriod(savingsAccount, interestStartDate, interestEndDate);
                BigDecimal balanceToAdd = interestCalculationHelper.calculateBalanceInterest(minimalActualBalanceForPeriod, savingsAccount.getTariff());
                balanceService.updateBalance(savingsAccount.getAccount().getId(), createTransactionDtoForInterest(balanceToAdd));
                savingsAccount.setLastInterestDate(LocalDateTime.now());
                savingsAccountRepository.save(savingsAccount);
                log.info("Interest accrual completed for Savings account with ID={}. Amount added: {}", savingsAccount.getId(), balanceToAdd);
            }
            else {
                log.info("Accruing interest for Savings account with ID={} was not done. Reason: Interest period condition not met", savingsAccount.getId());
            }
        } catch (StaleStateException ex) {
            log.error("savings account ID={}, version={}", savingsAccount.getId(), savingsAccount.getVersion());
        }
    }

    private boolean canAccrueInterest(SavingsAccount savingsAccount, LocalDateTime interestStartDate) {
        return (savingsAccount.getLastInterestDate() != null && !savingsAccount.getLastInterestDate().isAfter(interestStartDate)) ||
                (savingsAccount.getLastInterestDate() == null && !savingsAccount.getCreatedAt().isAfter(interestStartDate));
    }

    private BigDecimal getMinimalActualBalanceForPeriod(
            SavingsAccount savingsAccount,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        Optional<BigDecimal> pendingMinimalActualBalanceForPeriod = balanceAuditService
                .findMinimalActualBalanceByAccountAndPeriod(savingsAccount.getAccount().getId(), startDate, endDate);
        return pendingMinimalActualBalanceForPeriod.orElse(savingsAccount.getAccount().getBalance().getActualBalance());
    }

    private TransactionDto createTransactionDtoForInterest(BigDecimal amountToAdd) {
        return TransactionDto.builder()
                .operationId(1L)
                .amount(amountToAdd)
                .operationType(OperationType.CLEARING)
                .build();
    }

    private void validateAccountType(Account account) {
        if (account.getType() != AccountType.SAVINGS) {
            throw new IllegalArgumentException(
                    "Unable to create a savings account with the account of type '%s'. Expected account type is 'SAVINGS'."
                            .formatted(account.getType())
            );
        }
    }

    private SavingsAccount getSavingsAccount(long savingsAccountId) {
        return savingsAccountRepository.findById(savingsAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Savings account with ID=%d was not found".formatted(savingsAccountId)));
    }

    private void handleDataIntegrityViolationException(DataIntegrityViolationException ex, long baseAccountId) {
        if (ex.getMessage().contains(ACCOUNT_UNIQUE_CONSTRAINT)) {
            String exceptionMessage = """
                Unable to set base account ID='%d' for new savings account: \
                there is already an existing savings account with this base account ID.
            """.formatted(baseAccountId);
            throw new UniqueConstraintException(exceptionMessage, ex);
        }
        throw ex;
    }
}