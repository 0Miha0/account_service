package faang.school.accountservice.service.request_task;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.repository.RequestTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestTaskServiceTest {

    @Mock
    private RequestTaskRepository requestTaskRepository;

    @InjectMocks
    private RequestTaskService requestTaskService;

    @Test
    public void createRequestTasksForRequestTest(){
        Request request = Request.builder()
                .idempotentToken(UUID.randomUUID())
                .requestType(RequestType.CREATE_ACCOUNT)
                .build();

       RequestTask requestTask1 = RequestTask.builder()
                .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                .status(RequestTaskStatus.AWAITING)
                .request(request)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .status(RequestTaskStatus.AWAITING)
                .request(request)
                .build();

        RequestTask requestTask3 = RequestTask.builder()
                .handler(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT)
                .status(RequestTaskStatus.AWAITING)
                .request(request)
                .build();

        RequestTask requestTask4 = RequestTask.builder()
                .handler(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION)
                .status(RequestTaskStatus.AWAITING)
                .request(request)
                .build();
        List<RequestTask> requestTasks = new ArrayList<>(
                List.of(requestTask1,requestTask2,requestTask3,requestTask4));

        requestTaskService.createRequestTasksForRequest(request);

        verify(requestTaskRepository).saveAll(requestTasks);
    }
}