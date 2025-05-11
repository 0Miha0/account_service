package faang.school.accountservice.scheduler.request;

import faang.school.accountservice.service.request.ScheduledRequestExecutorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestSchedulerService {

    private static final int SCHEDULING_RATE_MILLISECONDS = 500;

    private final ScheduledRequestExecutorService scheduledExecutorService;

    @PostConstruct
    @Scheduled(fixedRate = SCHEDULING_RATE_MILLISECONDS)
    public void executeRequests() {
        scheduledExecutorService.execute();
    }
}
