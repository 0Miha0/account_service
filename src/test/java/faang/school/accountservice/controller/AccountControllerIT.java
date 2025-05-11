package faang.school.accountservice.controller;

import faang.school.accountservice.dto.AccountOwnerRequest;
import faang.school.accountservice.dto.AccountOwnerResponse;
import faang.school.accountservice.dto.AccountRequest;
import faang.school.accountservice.entity.Request;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.repository.RequestRepository;
import faang.school.accountservice.service.AccountService;
import faang.school.accountservice.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccountControllerIT extends BaseContextTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RequestRepository requestRepository;

    @Test
    void createAccountRequestTest() throws Exception {
        Long ownerId = createOwner(1L, OwnerType.USER);
        String accountRequest = objectMapper.writeValueAsString(
                new AccountRequest(AccountType.LEGAL, Currency.RUB, ownerId, OwnerType.USER, LocalDateTime.now())
        );
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", ownerId)
                        .content(accountRequest))
                .andExpect(status().isOk());

        Request request = requestRepository.findAll().get(0);

        assertNotNull(request);
        assertEquals(RequestType.CREATE_ACCOUNT, request.getRequestType());
        assertEquals(accountRequest, request.getContext());
    }

    @Test
    void getAccountTest() throws Exception {
        Long ownerId = createOwner(1L, OwnerType.PROJECT);
        Long accountId = createAccount(AccountType.LEGAL, Currency.RUB, ownerId, OwnerType.PROJECT, LocalDateTime.now());
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .header("x-user-id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AccountStatus.ACTIVE.toString()))
                .andExpect(jsonPath("$.id").value(accountId));
    }

    @Test
    void blockAccountTest() throws Exception {
        Long ownerId = createOwner(2L, OwnerType.USER);
        Long accountId = createAccount(AccountType.LEGAL, Currency.RUB, ownerId, OwnerType.USER, LocalDateTime.now());
        mockMvc.perform(put("/api/v1/accounts/{id}/block", accountId)
                        .header("x-user-id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AccountStatus.BLOCKED.toString()));
    }

    @Test
    void closeAccountTest() throws Exception {
        Long ownerId = createOwner(2L, OwnerType.PROJECT);
        Long accountId = createAccount(AccountType.LEGAL, Currency.RUB, ownerId, OwnerType.PROJECT, LocalDateTime.now());
        mockMvc.perform(put("/api/v1/accounts/{id}/close", accountId)
                        .header("x-user-id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AccountStatus.CLOSED.toString()));
    }

    private Long createOwner(Long ownerId, OwnerType ownerType) throws Exception {
        String ownerRequest = objectMapper.writeValueAsString(new AccountOwnerRequest(ownerId, ownerType));

        MvcResult result = mockMvc.perform(post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", ownerId)
                        .content(ownerRequest))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AccountOwnerResponse ownerResponse = objectMapper.readValue(responseBody, AccountOwnerResponse.class);
        return ownerResponse.getOwnerId();
    }

    private Long createAccount(AccountType type, Currency currency, Long ownerId,
                               OwnerType ownerType, LocalDateTime scheduledAt) throws Exception {

        String accountRequest = objectMapper.writeValueAsString(
                new AccountRequest(type, currency, ownerId, ownerType, scheduledAt));

        Request request = Request.builder()
                .context(String.valueOf(accountRequest))
                .build();

        Account account = accountService.createAccount(request);

        return account.getId();
    }
}
