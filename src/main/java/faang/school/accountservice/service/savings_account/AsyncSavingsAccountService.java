package faang.school.accountservice.service.savings_account;

import faang.school.accountservice.entity.savings_account.SavingsAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncSavingsAccountService {

    private final SavingsAccountService savingsAccountService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async("taskExecutor")
    public CompletableFuture<Void> accrueInterest(List<SavingsAccount> savingsAccounts) {
        savingsAccounts.forEach(savingsAccountService::accrueInterest);
        return CompletableFuture.completedFuture(null);
    }
}
