package faang.school.accountservice.service;

import faang.school.accountservice.entity.AccountOwner;
import faang.school.accountservice.repository.AccountOwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountOwnerService {

    private final AccountOwnerRepository accountOwnerRepository;

    public AccountOwner findById(Long id) {
        return accountOwnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account owner with id: " + id + " not found"));
    }
}
