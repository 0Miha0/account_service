package faang.school.accountservice.service.calculators;

import faang.school.accountservice.entity.tariff.Tariff;
import faang.school.accountservice.enums.tariff.InterestPeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class InterestCalculationHelperTest {

    @InjectMocks
    private InterestCalculationHelper calculationHelper;

    @Test
    void getInterestStartDateTest() {
        InterestPeriod interestPeriod = InterestPeriod.WEEKLY;
        LocalDateTime endDate = LocalDate.of(2025, 1, 8).atStartOfDay();
        LocalDateTime expectedStartDate = LocalDate.of(2025, 1, 1).atStartOfDay();

        LocalDateTime startDate = calculationHelper.getInterestStartDate(endDate, interestPeriod);

        assertEquals(expectedStartDate, startDate);
    }

    @Test
    void calculateBalanceInterestTest() {
        BigDecimal tariffRate = BigDecimal.valueOf(15);
        Tariff tariff = Tariff.builder()
                .interestPeriod(InterestPeriod.ANNUALLY)
                .currentRate(tariffRate)
                .build();
        BigDecimal currentBalance = BigDecimal.valueOf(10000);
        BigDecimal expectedBalanceToAdd = new BigDecimal(1500);

        BigDecimal balanceToAdd = calculationHelper.calculateBalanceInterest(currentBalance, tariff);

        assertEquals(0, expectedBalanceToAdd.compareTo(balanceToAdd));
    }
}