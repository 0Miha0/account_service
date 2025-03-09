package faang.school.accountservice.service;

import faang.school.accountservice.enums.AccountType;
import org.springframework.stereotype.Service;

@Service
public class FreeAccountNumberService {

    public String getFreeAccountNumber(AccountType accountType) {
        // Implement logic to generate a free account number
        return "free-account-number-" + System.currentTimeMillis();
    }
}
