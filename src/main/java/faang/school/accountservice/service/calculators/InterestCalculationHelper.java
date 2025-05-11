package faang.school.accountservice.service.calculators;

import faang.school.accountservice.entity.tariff.Tariff;
import faang.school.accountservice.enums.tariff.InterestPeriod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class InterestCalculationHelper {

    private static final BigDecimal ONE_HUNDRED_PERCENT = new BigDecimal("100");
    private static final byte WEEKS_IN_YEAR = 52;
    private static final byte MONTHS_IN_YEAR = 12;
    private static final byte QUARTERS_IN_YEAR = 4;

    private final Map<InterestPeriod, Function<LocalDateTime, LocalDateTime>> interestPeriodsToStartDateCalculators;
    private final Map<InterestPeriod, Byte> interestPeriodsToDivisors;

    public InterestCalculationHelper() {
        interestPeriodsToStartDateCalculators = new EnumMap<>(InterestPeriod.class);
        interestPeriodsToStartDateCalculators.put(InterestPeriod.WEEKLY, endDate -> endDate.minusWeeks(1));
        interestPeriodsToStartDateCalculators.put(InterestPeriod.MONTHLY, endDate -> endDate.minusMonths(1));
        interestPeriodsToStartDateCalculators.put(InterestPeriod.QUARTERLY, endDate -> endDate.minusMonths(3));
        interestPeriodsToStartDateCalculators.put(InterestPeriod.ANNUALLY, endDate -> endDate.minusYears(1));

        interestPeriodsToDivisors = new EnumMap<>(InterestPeriod.class);
        interestPeriodsToDivisors.put(InterestPeriod.WEEKLY, WEEKS_IN_YEAR);
        interestPeriodsToDivisors.put(InterestPeriod.MONTHLY, MONTHS_IN_YEAR);
        interestPeriodsToDivisors.put(InterestPeriod.QUARTERLY, QUARTERS_IN_YEAR);
        interestPeriodsToDivisors.put(InterestPeriod.ANNUALLY, (byte) 1);
    }

    public LocalDateTime getInterestStartDate(LocalDateTime endDate, InterestPeriod interestPeriod) {
        return interestPeriodsToStartDateCalculators.get(interestPeriod).apply(endDate);
    }

    public BigDecimal calculateBalanceInterest(BigDecimal startBalance, Tariff tariff) {
        BigDecimal currentRate = tariff.getCurrentRate();
        byte divisor = getDivisorForInterestPeriod(tariff.getInterestPeriod());
        return startBalance.multiply(currentRate.divide(new BigDecimal(divisor), MathContext.DECIMAL64))
                .divide(ONE_HUNDRED_PERCENT, MathContext.DECIMAL64);
    }

    private byte getDivisorForInterestPeriod(InterestPeriod interestPeriod) {
        return interestPeriodsToDivisors.get(interestPeriod);
    }
}
