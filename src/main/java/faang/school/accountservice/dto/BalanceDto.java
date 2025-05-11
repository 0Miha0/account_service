package faang.school.accountservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDto {

    private Long id;

    private BigDecimal authorizationBalance;

    private BigDecimal actualBalance;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private int version;

    private AccountResponse account;
}