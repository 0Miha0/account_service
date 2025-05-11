package faang.school.accountservice.exception;

import java.math.BigDecimal;

public class BalanceBelowZeroException extends RuntimeException{
    public BalanceBelowZeroException(long accountId, BigDecimal amount) {
        super("In account " + accountId + " not enough money to be debited " + amount);
    }
}