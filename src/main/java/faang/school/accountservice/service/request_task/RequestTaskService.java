package faang.school.accountservice.service.request_task;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.repository.RequestTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTaskService {

    private final RequestTaskRepository requestTaskRepository;

    public List<RequestTask> createRequestTasksForRequest(Request request) {
        List<RequestTask> requestTasks = new ArrayList<>();
        List<RequestTaskType> requestTasksTypes =
                getRequestTaskTypesByRequestType(request.getRequestType());

        requestTasksTypes.forEach(requestTaskType ->
                requestTasks.add(RequestTask.builder()
                        .handler(requestTaskType)
                        .status(RequestTaskStatus.AWAITING)
                        .request(request)
                        .build()
                ));
        requestTaskRepository.saveAll(requestTasks);
        log.info("Created {} RequestTasks for request with id: {}, request type: {}",
                requestTasks.size(), request.getIdempotentToken(), request.getRequestType());
        return requestTasks;
    }

    private List<RequestTaskType> getRequestTaskTypesByRequestType(RequestType requestType) {
        List<RequestTaskType> requestTaskTypes = new ArrayList<>();
        switch (requestType) {
            case CREATE_ACCOUNT -> requestTaskTypes = List.of(RequestTaskType.CHECK_ACCOUNTS_QUANTITY,
                    RequestTaskType.WRITE_INTO_ACCOUNT, RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT,
                    RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION);
        }
        return requestTaskTypes;
    }
}
