package faang.school.accountservice.service.request;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.repository.RequestRepository;
import faang.school.accountservice.request_executor.RequestProcessExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledRequestExecutorService {

    private final RequestExecutorService requestExecutorService;
    private final RequestRepository requestRepository;
    private final List<RequestProcessExecutor> processExecutors;

    public void execute() {
        List<Request> requests = requestRepository.findAllAwaitingRequests();
        requests.stream()
                .collect(Collectors.groupingBy(Request::getRequestType))
                .forEach((requestType, requestsList) ->
                        processExecutors.forEach(processExecutor -> {
                            if (requestType.equals(processExecutor.getRequestType())) {
                                requestsList.forEach(request ->
                                        processExecutor.getThreadPoolExecutor().execute(() ->
                                                requestExecutorService.executeRequest(request)));
                            }
                        }));
    }
}
