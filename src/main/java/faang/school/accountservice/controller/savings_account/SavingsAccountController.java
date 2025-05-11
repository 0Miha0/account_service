package faang.school.accountservice.controller.savings_account;

import faang.school.accountservice.dto.savings_account.SavingsAccountCreateDto;
import faang.school.accountservice.dto.savings_account.SavingsAccountResponse;
import faang.school.accountservice.service.savings_account.SavingsAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/savings-accounts")
@RequiredArgsConstructor
@Validated
public class SavingsAccountController {

    private final SavingsAccountService savingsAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsAccountResponse createSavingsAccount(@Valid @RequestBody SavingsAccountCreateDto createDto) {
        return savingsAccountService.createSavingsAccount(createDto);
    }

    @PatchMapping("/{savingsAccountId}/tariffs")
    public SavingsAccountResponse updateSavingsAccountTariff(
            @PathVariable @Min(1) long savingsAccountId,
            @RequestParam @Min(1) long tariffId
    ) {
        return savingsAccountService.updateSavingsAccountTariff(savingsAccountId, tariffId);
    }

    @GetMapping("/{savingsAccountId}")
    public SavingsAccountResponse getSavingsAccountById(@PathVariable @Min(1) long savingsAccountId) {
        return savingsAccountService.getSavingsAccountById(savingsAccountId);
    }
}
