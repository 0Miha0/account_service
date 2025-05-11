package faang.school.accountservice.dto.savings_account;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavingsAccountCreateDto {

    @NotNull(message = "Base account ID must be specified")
    @Min(value = 1)
    private long baseAccountId;

    @NotNull(message = "Initial tariff ID must be specified")
    @Min(value = 1)
    private long tariffId;
}
