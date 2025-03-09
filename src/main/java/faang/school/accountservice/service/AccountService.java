package faang.school.accountservice.service;

import faang.school.accountservice.enums.AccountStatus;
import faang.school.accountservice.repository.AccountRepository;
import faang.school.accountservice.dto.AccountDto;
import faang.school.accountservice.entity.Account;
import faang.school.accountservice.mapper.AccountMapper;
import faang.school.accountservice.validator.AccountValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountValidator accountValidator;
    private final FreeAccountNumberService numberService;
    private final AccountOwnerService accountOwnerService;

    @Transactional(readOnly = true)
    public AccountDto getAccount(Long id) {
        log.info("Get account with id: {}", id);
        return accountMapper.toDto(findById(id));
    }

    public AccountDto openAccount(AccountDto dto) {
        log.info("Open account with: {}", dto);
        Account account = Account.builder()
                .accountOwner(accountOwnerService.findById(dto.getOwnerId()))
                .accountNumber(numberService.getFreeAccountNumber(dto.getType()))
                .type(dto.getType())
                .currency(dto.getCurrency())
                .status(AccountStatus.ACTIVE)
                .build();
        saveAccount(account);
        log.info("Account created with id: {}", account.getId());
        return  accountMapper.toDto(account);
    }

    public AccountDto blockAccount(Long id) {
        log.info("Block account with id: {}", id);
        Account account = findById(id);
        accountValidator.isItBlocked(account.getStatus(), "The account was already a blockage");
        account.setStatus(AccountStatus.BLOCKED);
        saveAccount(account);
        log.info("Account blocked with id: {}", account.getId());
        return accountMapper.toDto(account);
    }

    public AccountDto closeAccount(Long id) {
        log.info("Close account with id: {}", id);
        Account account = findById(id);
        accountValidator.isItClosed(account.getStatus(), "The account was already closed");
        account.setStatus(AccountStatus.CLOSED);
        saveAccount(account);
        log.info("Account closed with id: {}", account.getId());
        return accountMapper.toDto(account);
    }

    public Account findById(Long id) {
        log.info("Find account by id: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account with id: " + id + " not found"));
    }

    public void saveAccount(Account account) {
        log.info("Save account: {}", account);
        accountRepository.save(account);
        log.info("Account saved with id: {}", account.getId());
    }
}
