package faang.school.accountservice.validator;

import faang.school.accountservice.entity.Account;
import faang.school.accountservice.enums.AccountStatus;
import faang.school.accountservice.exception.customExceptions.DataValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountValidator {

    public void isItBlocked(AccountStatus status, String message) {
        if(status == AccountStatus.BLOCKED) {
            log.info(message);
            throw new DataValidationException(message);
        }
    }

    public void isItClosed(AccountStatus status, String message) {
        if(status == AccountStatus.CLOSED) {
            log.info(message);
            throw new DataValidationException(message);
        }
    }
}
