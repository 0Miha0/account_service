package faang.school.accountservice.scheduler;

import faang.school.accountservice.repository.FreeAccountNumbersRepository;
import faang.school.accountservice.scheduler.free_account_number.FreeAccountNumberScheduler;
import faang.school.accountservice.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FreeAccountNumberSchedulerTest extends BaseContextTest {

    @Autowired
    FreeAccountNumberScheduler freeAccountNumberScheduler;

    @Autowired
    FreeAccountNumbersRepository accountNumbersRepository;

    @Test
    void ensureFreeAccountNumbersTest() {
        assertDoesNotThrow(() -> freeAccountNumberScheduler.ensureFreeAccountNumbers());
        assertEquals(5, accountNumbersRepository.countByAccountType(AccountType.INDIVIDUAL));
        assertEquals(6, accountNumbersRepository.countByAccountType(AccountType.LEGAL));
        assertEquals(7, accountNumbersRepository.countByAccountType(AccountType.SAVINGS));
        assertEquals(8, accountNumbersRepository.countByAccountType(AccountType.DEBIT));
    }
}