package faang.school.accountservice.service.request;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.repository.RequestRepository;
import faang.school.accountservice.request_executor.RequestProcessExecutor;
import faang.school.accountservice.request_executor.impl.CreateAccountExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledRequestExecutorServiceTest {

    @Mock
    private RequestExecutorService requestExecutorService;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private CreateAccountExecutor createAccountExecutor;

    private ScheduledRequestExecutorService executorService;

    @BeforeEach
    public void setUp() {
        List<RequestProcessExecutor> executors = new ArrayList<>(List.of(createAccountExecutor));
        executorService = new ScheduledRequestExecutorService(
                requestExecutorService, requestRepository, executors);
    }

    @Test
    public void executeTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        Request request1 = Request.builder()
                .idempotentToken(UUID.randomUUID())
                .requestType(RequestType.CREATE_ACCOUNT)
                .build();

        Request request2 = Request.builder()
                .idempotentToken(UUID.randomUUID())
                .requestType(RequestType.CREATE_ACCOUNT)
                .build();

        Request request3 = Request.builder()
                .idempotentToken(UUID.randomUUID())
                .requestType(RequestType.CREATE_ACCOUNT)
                .build();

        List<Request> requests = new ArrayList<>(List.of(request1, request2, request3));

        when(requestRepository.findAllAwaitingRequests()).thenReturn(requests);
        when(createAccountExecutor.getRequestType()).thenReturn(RequestType.CREATE_ACCOUNT);
        when(createAccountExecutor.getThreadPoolExecutor()).thenReturn(Executors.newSingleThreadExecutor());

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(requestExecutorService).executeRequest(any(Request.class));

        executorService.execute();
        latch.await();

        verify(requestExecutorService, times(1)).executeRequest(request1);
        verify(requestExecutorService, times(1)).executeRequest(request2);
        verify(requestExecutorService, times(1)).executeRequest(request3);
    }
}