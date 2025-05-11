package faang.school.accountservice.service.request;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.repository.RequestRepository;
import faang.school.accountservice.service.request_task.RequestTaskService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestTaskService requestTaskService;

    @InjectMocks
    private RequestService requestService;

    @Test
    public void getRequestStatusTest() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Request request = Request.builder()
                .idempotentToken(id)
                .requestStatus(RequestStatus.PROCESSING)
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(request));

        RequestStatus requestStatus = requestService.getRequestStatus(id);

        verify(requestRepository).findById(id);
        assertEquals(RequestStatus.PROCESSING, requestStatus);
    }

    @Test
    public void getRequestStatusThrowsExceptionTest() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        when(requestRepository.findById(id)).thenThrow(EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class, () -> requestService.getRequestStatus(id));
    }

    @Test
    public void updateRequestTest() {
        Request request = Request.builder().build();

        when(requestRepository.save(request)).thenReturn(request);

        requestService.updateRequest(request);

        verify(requestRepository).save(request);
    }

    @Test
    public void createRequestTest() {
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        RequestTask requestTask1 = RequestTask.builder().build();
        RequestTask requestTask2 = RequestTask.builder().build();
        List<RequestTask> requestTasks = new ArrayList<>(List.of(requestTask1, requestTask2));

        when(requestTaskService.createRequestTasksForRequest(any())).thenReturn(requestTasks);

        Request result = requestService.createRequest(RequestType.CREATE_ACCOUNT, LocalDateTime.now());

        verify(requestRepository).save(captor.capture());
        Request request = captor.getValue();
        verify(requestTaskService).createRequestTasksForRequest(request);

        assertNotNull(result);
        assertEquals(RequestStatus.AWAITING, result.getRequestStatus());
        assertEquals(RequestType.CREATE_ACCOUNT, result.getRequestType());
        assertEquals(2, result.getRequestTasks().size());
    }
}