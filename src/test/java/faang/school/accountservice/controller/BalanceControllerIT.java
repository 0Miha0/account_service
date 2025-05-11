package faang.school.accountservice.controller;

import faang.school.accountservice.dto.BalanceDto;
import faang.school.accountservice.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/db/Create_balance_for_test.sql")
public class BalanceControllerIT extends BaseContextTest {

    @Test
    public void getBalanceTest() throws Exception {
        int accountId = 1;
        BalanceDto expectedBalance = new BalanceDto();
        expectedBalance.setAuthorizationBalance(BigDecimal.valueOf(53.00));
        expectedBalance.setActualBalance(BigDecimal.valueOf(53.00));

        MvcResult result = mockMvc.perform(get("/api/v1/account/" + accountId + "/balance")
                        .header("x-user-id", 1)
                ).andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BalanceDto actualBalance = objectMapper.readValue(responseBody, BalanceDto.class);
        assertEquals(accountId, actualBalance.getAccount().getId());
        assertEquals(0, expectedBalance.getAuthorizationBalance().compareTo(actualBalance.getAuthorizationBalance()));
        assertEquals(0, expectedBalance.getActualBalance().compareTo(actualBalance.getActualBalance()));
    }
}
