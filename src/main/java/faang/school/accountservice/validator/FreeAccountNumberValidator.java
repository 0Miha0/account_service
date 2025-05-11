package faang.school.accountservice.validator;

import org.springframework.stereotype.Component;

@Component
public class FreeAccountNumberValidator {

    public void validateNumberSequenceIsNotExceeded(long numberSequence,
                                                    int accountNumberLength,
                                                    int accountTypeIdentity) {
        if (Long.toString(numberSequence).length() >
                (accountNumberLength - Integer.toString(accountTypeIdentity).length())) {
            throw new InternalError("Quantity numbers of sequence exceeded");
        }
    }
}