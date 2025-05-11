package faang.school.accountservice.service.request;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.repository.RequestRepository;
import faang.school.accountservice.repository.RequestTaskRepository;
import faang.school.accountservice.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@Sql(scripts = "/db/request-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RequestServiceIT extends BaseContextTest {

    @Autowired
    RequestTaskRepository requestTaskRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private RequestRepository requestRepository;

    @Test
    public void getRequestStatusTest() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        RequestStatus requestStatus = requestService.getRequestStatus(id);

        assertEquals(RequestStatus.DONE, requestStatus);
    }

    @Test
    public void updateRequestTest() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        Request request = Request.builder()
                .idempotentToken(id)
                .requestType(RequestType.CREATE_ACCOUNT)
                .requestStatus(RequestStatus.AWAITING)
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context("Context")
                .build();

        requestTask1.setRequest(request);
        requestTask2.setRequest(request);

        requestService.updateRequest(request);

        Request updatedRequest = requestRepository.findById(id).orElse(new Request());
        assertEquals(RequestStatus.AWAITING, updatedRequest.getRequestStatus());
        assertEquals("Context", updatedRequest.getContext());
        assertEquals(RequestStatus.AWAITING, updatedRequest.getRequestStatus());
    }

    @Test
    public void createRequestTest() {
        RequestType requestType = RequestType.CREATE_ACCOUNT;

        Request request = requestService.createRequest(requestType, null);

        Request savedRequest = requestRepository.findById(request.getIdempotentToken()).orElse(new Request());
        List<Long> tasksIds = request.getRequestTasks().stream()
                .map(RequestTask::getId).toList();
        List<RequestTask> createdTasks = requestTaskRepository.findAllById(tasksIds);

        assertEquals(requestType, savedRequest.getRequestType());
        assertEquals(RequestStatus.AWAITING, savedRequest.getRequestStatus());
        assertEquals(4, createdTasks.size());
    }
}