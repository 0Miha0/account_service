package faang.school.accountservice.scheduler.savings_account;

import faang.school.accountservice.entity.savings_account.SavingsAccount;
import faang.school.accountservice.enums.AccountStatus;
import faang.school.accountservice.repository.savings_account.SavingsAccountRepository;
import faang.school.accountservice.service.savings_account.AsyncSavingsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsAccountScheduler {

    private final AsyncSavingsAccountService asyncSavingsAccountService;
    private final SavingsAccountRepository savingsAccountRepository;

    @Value("${account.savings.accruing-interest.batch-size}")
    private int accruingInterestBatchSize;

    @Transactional
    @Scheduled(cron = "${account.savings.accruing-interest.cron}")
    public void startAccruingInterest() {
        log.info("Starting accruing interest for savings accounts...");
        List<SavingsAccount> savingsAccounts = savingsAccountRepository.getSavingsAccountsByStatus(AccountStatus.ACTIVE);
        log.info("Found {} active savings accounts.", savingsAccounts.size());

        List<List<SavingsAccount>> savingsAccountsBatches = divideSavingsAccountsIntoBatches(savingsAccounts);
        List<CompletableFuture<Void>> futures = savingsAccountsBatches.stream()
                .map(batch -> asyncSavingsAccountService.accrueInterest(batch)
                        .exceptionally(ex -> {
                            log.error(
                                    "Error while accruing interest for savings accounts with IDs: {}",
                                    batch.stream().map(SavingsAccount::getId).toList(),
                                    ex);
                            return null;
                        })
                ).toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                        .join();
        log.info("Interest accruing for savings accounts was finished");
    }

    private List<List<SavingsAccount>> divideSavingsAccountsIntoBatches(List<SavingsAccount> savingsAccounts) {
        List<List<SavingsAccount>> savingsAccountsBatches = new ArrayList<>();
        for (int i = 0; i < savingsAccounts.size(); i += accruingInterestBatchSize) {
            savingsAccountsBatches.add(savingsAccounts.subList(i, Math.min(i + accruingInterestBatchSize, savingsAccounts.size())));
        }
        return savingsAccountsBatches;
    }
}
