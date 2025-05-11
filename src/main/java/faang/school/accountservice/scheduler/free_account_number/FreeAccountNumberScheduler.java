package faang.school.accountservice.scheduler.free_account_number;

import faang.school.accountservice.properties.FreeAccountNumbersGenerationProperties;
import faang.school.accountservice.service.FreeAccountNumbersService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FreeAccountNumberScheduler {

    private final FreeAccountNumbersGenerationProperties numbersProperties;
    private final FreeAccountNumbersService freeAccountNumbersService;

    @Scheduled(cron = "${account.number.generation.cron}")
    public void ensureFreeAccountNumbers() {
        numbersProperties.getMaxAmountByType().forEach(freeAccountNumbersService::ensureFreeAccountNumbers);
    }
}