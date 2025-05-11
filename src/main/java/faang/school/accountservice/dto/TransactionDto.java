package faang.school.accountservice.dto;

import faang.school.accountservice.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private Long operationId;

    private BigDecimal amount;

    private OperationType operationType;
}
