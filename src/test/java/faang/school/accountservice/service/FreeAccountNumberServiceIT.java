package faang.school.accountservice.service;

import faang.school.accountservice.entity.FreeAccountNumber;
import faang.school.accountservice.repository.FreeAccountNumbersRepository;
import faang.school.accountservice.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FreeAccountNumberServiceIT extends BaseContextTest {

    @Autowired
    FreeAccountNumbersService freeAccountNumbersService;

    @Autowired
    FreeAccountNumbersRepository accountNumbersRepository;

    @Test
    public void getFreeAccountNumberTest() {
        AccountType accountType = AccountType.DEBIT;

        String resultNumber = freeAccountNumbersService.getFreeAccountNumber(accountType);
        assertNotNull(resultNumber);
        assertEquals("6555000000000001", resultNumber);
    }

    @Test
    public void generateFreeAccountNumbersTest() {
        Set<String> expectedAccountNumbers = Set.of(
                "5444000000000001",
                "5444000000000002",
                "5444000000000003",
                "5444000000000004",
                "5444000000000005"
        );
        int batchSize = 5;
        AccountType accountType = AccountType.SAVINGS;

        freeAccountNumbersService.generateFreeAccountNumbers(accountType, batchSize);

        for (int i = 1; i <= batchSize; i++) {
            FreeAccountNumber result = accountNumbersRepository.retrieveFreeAccountNumber(accountType.name());
            assertNotNull(result);
            assertTrue(expectedAccountNumbers.contains(result.getAccountNumber()));
        }
    }

    @Test
    void ensureFreeAccountNumbersTest() {
        freeAccountNumbersService.generateFreeAccountNumbers(AccountType.INDIVIDUAL, 1);
        long threshold = 4;
        assertDoesNotThrow(() -> freeAccountNumbersService.ensureFreeAccountNumbers(AccountType.INDIVIDUAL, threshold));

        long individualNumbersAmount = accountNumbersRepository.countByAccountType(AccountType.INDIVIDUAL);
        assertEquals(threshold, individualNumbersAmount);
    }
}