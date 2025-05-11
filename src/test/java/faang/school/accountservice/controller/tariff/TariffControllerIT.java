package faang.school.accountservice.controller.tariff;

import com.fasterxml.jackson.core.type.TypeReference;
import faang.school.accountservice.dto.tariff.TariffCreateDto;
import faang.school.accountservice.dto.tariff.TariffResponse;
import faang.school.accountservice.entity.tariff.Tariff;
import faang.school.accountservice.entity.tariff.TariffRateChangelog;
import faang.school.accountservice.enums.tariff.InterestPeriod;
import faang.school.accountservice.repository.tariff.TariffRateChangelogRepository;
import faang.school.accountservice.repository.tariff.TariffRepository;
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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/db/tariff/create_tariff_for_test.sql")
public class TariffControllerIT extends BaseContextTest {

    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private TariffRateChangelogRepository tariffRateChangelogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() throws DatabaseException {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "tariff_rate_changelog", "tariff");
        jdbcTemplate.execute("SELECT setval('tariff_id_seq', 1, false)");
        jdbcTemplate.execute("SELECT setval('tariff_rate_changelog_id_seq', 1, false)");
    }

    @Test
    void createTariffValidTest() throws Exception {
        long requesterId = 11L;
        String tariffName = "someTariff";
        InterestPeriod interestPeriod = InterestPeriod.WEEKLY;
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);
        String tariffRequest = objectMapper.writeValueAsString(new TariffCreateDto(tariffName, interestPeriod, tariffRate));

        MvcResult response = mockMvc.perform(post("/api/v1/tariffs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(("x-user-id"), requesterId)
                        .content(tariffRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        TariffResponse tariffResponse = objectMapper.readValue(responseBody, TariffResponse.class);

        Optional<Tariff> tariff = tariffRepository.findById(tariffResponse.getId());
        List<TariffRateChangelog> rateChangelogs = tariffRateChangelogRepository.findByTariffId(tariffResponse.getId());

        assertTrue(tariff.isPresent());
        assertEquals(interestPeriod, tariffResponse.getInterestPeriod());
        assertEquals(0, tariff.get().getCurrentRate().compareTo(tariffRate));
        assertEquals(1, rateChangelogs.size());
        assertEquals(0, rateChangelogs.get(0).getRate().compareTo(tariffRate));
        assertEquals(tariffName, tariffResponse.getName());
        assertEquals(0, tariffResponse.getCurrentRate().compareTo(tariffRate));
    }

    @Test
    void createTariffWithAlreadyExistingNameTest() throws Exception {
        long requesterId = 11L;
        String tariffName = "bonus";
        InterestPeriod interestPeriod = InterestPeriod.WEEKLY;
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);
        String tariffRequest = objectMapper.writeValueAsString(new TariffCreateDto(tariffName, interestPeriod, tariffRate));

        mockMvc.perform(post("/api/v1/tariffs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(("x-user-id"), requesterId)
                        .content(tariffRequest))
                .andExpect(status().isConflict());
    }

    @Test
    void updateTariffRate() throws Exception {
        long requesterId = 11L;
        String tariffName = "bonus";
        long tariffId = 1L;
        int expectedRateChangelogsAmount = 4;
        BigDecimal tariffRate = BigDecimal.valueOf(16);

        MvcResult response = mockMvc.perform(patch("/api/v1/tariffs/%d/rates?newRate=%s".formatted(tariffId, tariffRate))
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        TariffResponse tariffResponse = objectMapper.readValue(responseBody, TariffResponse.class);

        List<TariffRateChangelog> rateChangelogs = tariffRateChangelogRepository.findByTariffId(tariffId);
        int rateChangelogsSize = rateChangelogs.size();
        BigDecimal lastTariffRate = rateChangelogs.stream()
                .sorted(Comparator.comparing(TariffRateChangelog::getChangeDate).reversed())
                .toList().get(0).getRate();

        assertEquals(expectedRateChangelogsAmount, rateChangelogsSize);
        assertEquals(0, lastTariffRate.compareTo(tariffRate));
        assertEquals(tariffId, tariffResponse.getId());
        assertEquals(tariffName, tariffResponse.getName());
        assertEquals(0, tariffResponse.getCurrentRate().compareTo(tariffRate));
    }

    @Test
    void updateNotExistingTariffRateTest() throws Exception {
        long requesterId = 11L;
        long tariffId = 3L;
        BigDecimal tariffRate = BigDecimal.valueOf(16);

        mockMvc.perform(patch("/api/v1/tariffs/%d/rates?newRate=%s".formatted(tariffId, tariffRate))
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllTariffs() throws Exception {
        long requesterId = 11L;

        MvcResult response = mockMvc.perform(get("/api/v1/tariffs")
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        List<TariffResponse> tariffResponses = objectMapper.readValue(responseBody, new TypeReference<>(){});
        TariffResponse bonusTariff = tariffResponses.get(0);
        TariffResponse summerTariff = tariffResponses.get(1);

        assertEquals(2, tariffResponses.size());
        assertEquals(0, bonusTariff.getCurrentRate().compareTo(BigDecimal.valueOf(8)));
        assertEquals(0, summerTariff.getCurrentRate().compareTo(BigDecimal.valueOf(8)));
    }

    @Test
    void deleteTariff() throws Exception {
        long requesterId = 11L;
        long tariffId = 1L;
        int expectedRateChangelogsAmount = 0;

        mockMvc.perform(delete("/api/v1/tariffs/%d".formatted(tariffId))
                        .header(("x-user-id"), requesterId))
                .andExpect(status().isNoContent());

        List<Tariff> tariffResponses = tariffRepository.findAll();
        List<TariffRateChangelog> rateChangelogs = tariffRateChangelogRepository.findByTariffId(1L);

        assertEquals(expectedRateChangelogsAmount, rateChangelogs.size());
        assertEquals(1, tariffResponses.size());
    }
}