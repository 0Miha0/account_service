package faang.school.accountservice.dto.tariff;

import faang.school.accountservice.enums.tariff.InterestPeriod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffCreateDto {

    @NotBlank(message = "Tariff must be named")
    @Length(max = 128, message = "Max tariff name length = 128")
    private String name;

    @NotNull
    private InterestPeriod interestPeriod;

    @DecimalMin(value = "0.01", message = "Rate must be greater than or equal to 0.01")
    @DecimalMax(value = "99.99", message = "Rate must be less than or equal to 99.99")
    @Digits(integer = 2, fraction = 2, message = "Rate must have up to 2 digits and 2 decimals")
    @NotNull(message = "Tariff rate must be specified")
    private BigDecimal rate;
}
