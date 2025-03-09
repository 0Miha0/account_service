package faang.school.accountservice.controller;

import faang.school.accountservice.dto.AccountDto;
import faang.school.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Api for working with account")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Get account")
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable @Valid Long accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @Operation(summary = "Open account")
    @PostMapping
    public ResponseEntity<AccountDto> openAccount(@RequestBody AccountDto accountDto) {
        return ResponseEntity.ok(accountService.openAccount(accountDto));
    }

    @Operation(summary = "Block account")
    @PatchMapping("/{accountId}/block")
    public ResponseEntity<AccountDto> blockAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.blockAccount(accountId));
    }

    @Operation(summary = "Close account")
    @PatchMapping("/{accountId}/close")
    public ResponseEntity<AccountDto> closeAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.closeAccount(accountId));
    }
}
