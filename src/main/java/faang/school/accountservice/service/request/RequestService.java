package faang.school.accountservice.service.request;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.repository.RequestRepository;
import faang.school.accountservice.service.request_task.RequestTaskService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final RequestTaskService requestTaskService;

    public RequestStatus getRequestStatus(UUID requestId) {
        Request request = getRequest(requestId);
        log.info("Getting status for request with Id: {}", requestId);
        return request.getRequestStatus();
    }

    public void updateRequest(Request request) {
        requestRepository.save(request);
        log.info("Request with Id: {} updated", request.getIdempotentToken());
    }

    @Transactional
    public Request createRequest(RequestType requestType, LocalDateTime scheduledAt) {
        UUID requestId = UUID.randomUUID();

        Request request = Request.builder()
                .idempotentToken(requestId)
                .requestType(requestType)
                .requestStatus(RequestStatus.AWAITING)
                .scheduledAt(scheduledAt)
                .build();
        requestRepository.save(request);
        List<RequestTask> requestTasks = requestTaskService.createRequestTasksForRequest(request);
        request.setRequestTasks(requestTasks);
        log.info("Request with Id: {} created", request.getIdempotentToken());
        return request;
    }

    private Request getRequest(UUID requestId) {
        return requestRepository.findById(requestId).orElseThrow(
                () -> new EntityNotFoundException("Request with Id: " + requestId + " not found"));
    }
}
