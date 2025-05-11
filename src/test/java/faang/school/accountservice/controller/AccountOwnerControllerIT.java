package faang.school.accountservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import faang.school.accountservice.dto.AccountOwnerRequest;
import faang.school.accountservice.dto.savings_account.SavingsAccountResponse;
import faang.school.accountservice.util.BaseContextTest;
import liquibase.exception.DatabaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/db/savings_account/create_savings_account_for_test.sql")
public class AccountOwnerControllerIT extends BaseContextTest {
    private static final Long OWNER_ID = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() throws DatabaseException {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "account_owner", "account",
                "tariff", "tariff_rate_changelog", "savings_account");
        jdbcTemplate.execute("SELECT setval('tariff_id_seq', 1, false)");
        jdbcTemplate.execute("SELECT setval('tariff_rate_changelog_id_seq', 1, false)");
        jdbcTemplate.execute("SELECT setval('account_owner_id_seq', 1, false)");
        jdbcTemplate.execute("SELECT setval('account_id_seq', 1, false)");
        jdbcTemplate.execute("SELECT setval('savings_account_id_seq', 1, false)");
    }

    @Test
    void createOwnerTest() throws Exception {
        String ownerRequest = objectMapper.writeValueAsString(new AccountOwnerRequest(OWNER_ID, OwnerType.USER));

        mockMvc.perform(post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-user-id", OWNER_ID)
                        .content(ownerRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID))
                .andExpect(jsonPath("$.ownerType").value(OwnerType.USER.toString()));
    }

    @Test
    void getOwnerWithAccountsTest() throws Exception {
        mockMvc.perform(get("/api/v1/owners/search")
                        .header("x-user-id", OWNER_ID)
                        .param("ownerId", String.valueOf(OWNER_ID))
                        .param("ownerType", String.valueOf(OwnerType.PROJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID))
                .andExpect(jsonPath("$.accounts").isArray());
    }

    @Test
    void getSavingsAccountsByOwnerIdTest() throws Exception{
        long accountOwnerId = 1L;
        long requesterId = 1L;
        int expectedSavingsAccountAmount = 3;

        MvcResult response = mockMvc.perform(get("/api/v1/owners/%d/savings-accounts".formatted(accountOwnerId))
                        .header("x-user-id", requesterId))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        List<SavingsAccountResponse> savingsAccounts = objectMapper.readValue(responseBody, new TypeReference<>(){});

        assertEquals(expectedSavingsAccountAmount, savingsAccounts.size());
    }
}
