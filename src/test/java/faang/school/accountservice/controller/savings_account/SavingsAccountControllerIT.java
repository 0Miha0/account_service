package faang.school.accountservice.controller.savings_account;

import faang.school.accountservice.dto.savings_account.SavingsAccountCreateDto;
import faang.school.accountservice.dto.savings_account.SavingsAccountResponse;
import faang.school.accountservice.entity.savings_account.SavingsAccount;
import faang.school.accountservice.entity.savings_account.SavingsAccountTariffChangelog;
import faang.school.accountservice.repository.savings_account.SavingsAccountRepository;
import faang.school.accountservice.repository.savings_account.SavingsAccountTariffChangelogRepository;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/db/savings_account/create_savings_account_for_test.sql")
public class SavingsAccountControllerIT extends BaseContextTest {

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private SavingsAccountTariffChangelogRepository tariffChangelogRepository;

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
    void createSavingsAccountValidTest() throws Exception {
        long requesterId = 11L;
        long baseAccountId = 5L;
        long tariffId = 1L;
        int expectedAccountTariffChangelogAmount = 1;

        SavingsAccountCreateDto createDto = SavingsAccountCreateDto.builder()
                .baseAccountId(baseAccountId)
                .tariffId(tariffId)
                .build();
        String savingsAccountRequest = objectMapper.writeValueAsString(createDto);

        MvcResult response = mockMvc.perform(post("/api/v1/savings-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(savingsAccountRequest)
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        SavingsAccountResponse accountResponse = objectMapper.readValue(responseBody, SavingsAccountResponse.class);

        Optional<SavingsAccount> savingsAccount = savingsAccountRepository.findById(accountResponse.getId());
        List<SavingsAccountTariffChangelog> tariffChangelogs = tariffChangelogRepository.findBySavingsAccountId(accountResponse.getId());

        assertEquals(expectedAccountTariffChangelogAmount, tariffChangelogs.size());
        assertTrue(savingsAccount.isPresent());
        assertEquals(baseAccountId, savingsAccount.get().getAccount().getId());
        assertEquals(tariffId, savingsAccount.get().getTariff().getId());
        assertEquals(baseAccountId, accountResponse.getBaseAccountId());
        assertEquals(tariffId, accountResponse.getCurrentTariffId());
    }

    @Test
    void createSavingsAccountConflictTest() throws Exception {
        long requesterId = 11L;
        long baseAccountId = 1L;
        long tariffId = 1L;

        SavingsAccountCreateDto createDto = SavingsAccountCreateDto.builder()
                .baseAccountId(baseAccountId)
                .tariffId(tariffId)
                .build();
        String savingsAccountRequest = objectMapper.writeValueAsString(createDto);

        mockMvc.perform(post("/api/v1/savings-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(savingsAccountRequest)
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isConflict());
    }

    @Test
    void createSavingsAccountBasedOnNotSavingsAccountTest() throws Exception {
        long requesterId = 11L;
        long baseAccountId = 6L;
        long tariffId = 1L;

        SavingsAccountCreateDto createDto = SavingsAccountCreateDto.builder()
                .baseAccountId(baseAccountId)
                .tariffId(tariffId)
                .build();
        String savingsAccountRequest = objectMapper.writeValueAsString(createDto);

        mockMvc.perform(post("/api/v1/savings-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(savingsAccountRequest)
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSavingsAccountTariffValidTest() throws Exception {
        long savingsAccountId = 1L;
        long requesterId = 11L;
        long newTariffId = 2L;
        int expectedAccountTariffChangelogAmount = 3;

        MvcResult response = mockMvc.perform(patch("/api/v1/savings-accounts/%d/tariffs?tariffId=%d".formatted(savingsAccountId, newTariffId))
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        SavingsAccountResponse accountResponse = objectMapper.readValue(responseBody, SavingsAccountResponse.class);

        List<SavingsAccountTariffChangelog> tariffChangelogs = tariffChangelogRepository.findBySavingsAccountId(savingsAccountId);
        long lastTariffId = tariffChangelogs.stream()
                .sorted(Comparator.comparing(SavingsAccountTariffChangelog::getChangeDate).reversed())
                .toList().get(0).getTariff().getId();

        assertEquals(savingsAccountId, accountResponse.getId());
        assertEquals(newTariffId, accountResponse.getCurrentTariffId());
        assertEquals(expectedAccountTariffChangelogAmount, tariffChangelogs.size());
        assertEquals(newTariffId, lastTariffId);
    }

    @Test
    void updateNotExistingSavingsAccountTariffTest() throws Exception {
        long savingsAccountId = 5L;
        long requesterId = 11L;
        long newTariffId = 2L;

        mockMvc.perform(patch("/api/v1/savings-accounts/%d/tariffs?tariffId=%d".formatted(savingsAccountId, newTariffId))
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSavingsAccountByIdValidTest() throws Exception {
        long savingsAccountId = 1L;
        long requesterId = 11L;
        long expectedTariffId = 1L;
        long expectedBaseAccountId = 1L;

        MvcResult response = mockMvc.perform(get("/api/v1/savings-accounts/%d".formatted(savingsAccountId))
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        SavingsAccountResponse accountResponse = objectMapper.readValue(responseBody, SavingsAccountResponse.class);

        assertEquals(savingsAccountId, accountResponse.getId());
        assertEquals(expectedTariffId, accountResponse.getCurrentTariffId());
        assertEquals(expectedBaseAccountId, accountResponse.getBaseAccountId());
    }

    @Test
    void getSavingsAccountByIdNotFoundTest() throws Exception {
        long savingsAccountId = 5L;
        long requesterId = 11L;

        mockMvc.perform(get("/api/v1/savings-accounts/%d".formatted(savingsAccountId))
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isNotFound());
    }
}
