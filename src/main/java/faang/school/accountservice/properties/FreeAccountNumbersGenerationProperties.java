package faang.school.accountservice.properties;

import faang.school.accountservice.enums.AccountType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "account.number.generation")
public class FreeAccountNumbersGenerationProperties {

    private String cron;
    private Map<AccountType, Long> maxAmountByType;
}
