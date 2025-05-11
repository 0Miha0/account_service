package faang.school.accountservice.service;

import faang.school.accountservice.entity.AccountNumberSequence;
import faang.school.accountservice.entity.FreeAccountNumber;
import faang.school.accountservice.enums.AccountType;
import faang.school.accountservice.properties.AccountTypeIdentityProperties;
import faang.school.accountservice.properties.AccountTypeLengthProperties;
import faang.school.accountservice.repository.AccountNumbersSequenceRepository;
import faang.school.accountservice.repository.FreeAccountNumbersRepository;
import faang.school.accountservice.validator.FreeAccountNumberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreeAccountNumbersService {

    private final AccountTypeIdentityProperties identityProp;
    private final AccountTypeLengthProperties lengthProp;
    private final FreeAccountNumbersRepository freeAccountNumbersRepository;
    private final AccountNumbersSequenceRepository accountNumbersSequenceRepository;
    private final FreeAccountNumberValidator freeAccountNumberValidator;

    @Transactional
    public void generateFreeAccountNumbers(AccountType accountType, long accountNumbersAmount) {
        log.info("Start generating free account numbers for account type: {}", accountType);
        AccountNumberSequence sequence = accountNumbersSequenceRepository.incrementCounter(accountType.name(), accountNumbersAmount);
        long numberSequence = sequence.getCurrentSequenceValue();
        int accountNumberLength = getLengthByAccountType(accountType);
        int accountTypeIdentity = getNumberIdentityByAccountType(accountType);

        freeAccountNumberValidator.validateNumberSequenceIsNotExceeded(numberSequence,
                accountNumberLength, accountTypeIdentity);

        List<FreeAccountNumber> freeAccountNumbers = new ArrayList<>();
        for (long tempSequence = numberSequence - accountNumbersAmount + 1; tempSequence <= numberSequence; tempSequence++) {
            String accountNumber = buildAccountNumber(accountTypeIdentity, tempSequence, accountNumberLength);
            freeAccountNumbers.add(FreeAccountNumber.builder()
                    .accountType(accountType)
                    .accountNumber(accountNumber)
                    .build());
        }

        freeAccountNumbersRepository.saveAll(freeAccountNumbers);
        log.info("Finished generating free account numbers for account type: {}", accountType);
    }


    @Transactional
    public String getFreeAccountNumber(AccountType accountType) {
        log.info("Start getting free account number for account type: {}", accountType);
        FreeAccountNumber freeAccountNumber =
                freeAccountNumbersRepository.retrieveFreeAccountNumber(accountType.name());

        if (freeAccountNumber == null) {
            log.info("No free account number found for account type: {}.Generating new...", accountType);
            generateFreeAccountNumbers(accountType, 1);
            freeAccountNumber = freeAccountNumbersRepository.retrieveFreeAccountNumber(accountType.name());
        }
        log.info("Finished getting free account number for account type: {}", accountType);
        return freeAccountNumber.getAccountNumber();
    }

    @Transactional
    public void ensureFreeAccountNumbers(AccountType accountType, long threshold) {
        log.info("Ensuring free account numbers for account type: {}. Threshold: {}", accountType, threshold);
        long existingNumbersAmount = freeAccountNumbersRepository.countByAccountType(accountType);

        long amountDifference = threshold - existingNumbersAmount;
        if (amountDifference > 0) {
            generateFreeAccountNumbers(accountType, amountDifference);
            log.info("Generated {} new free account numbers for account type '{}'. Total now: {}",
                    amountDifference, accountType, threshold);
        } else {
            log.info("No new account numbers generated for account type '{}'. Threshold: {}, Existing: {}",
                    accountType, threshold, existingNumbersAmount);
        }
    }

    private int getLengthByAccountType(AccountType accountType) {
        return switch (accountType) {
            case INDIVIDUAL -> lengthProp.getIndividual();
            case LEGAL -> lengthProp.getLegal();
            case SAVINGS -> lengthProp.getSavings();
            case DEBIT -> lengthProp.getDebit();
        };
    }

    private int getNumberIdentityByAccountType(AccountType accountType) {
        return switch (accountType) {
            case INDIVIDUAL -> identityProp.getIndividual();
            case LEGAL -> identityProp.getLegal();
            case SAVINGS -> identityProp.getSavings();
            case DEBIT -> identityProp.getDebit();
        };
    }

    private String buildAccountNumber(int accountTypeIdentity, long uniqueNumber, int numberLength) {
        String characterForBuilding = "0";
        StringBuilder accountNumber = new StringBuilder();
        accountNumber.append(accountTypeIdentity);

        accountNumber.append((characterForBuilding).
                repeat(Math.max(0, (numberLength - String.valueOf(accountTypeIdentity).length()))));

        accountNumber.replace(accountNumber.length() - String.valueOf(uniqueNumber).length(),
                accountNumber.length(), String.valueOf(uniqueNumber));
        return accountNumber.toString();
    }
}