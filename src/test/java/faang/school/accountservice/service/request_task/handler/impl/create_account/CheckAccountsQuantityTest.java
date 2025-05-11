package faang.school.accountservice.service.request_task.handler.impl.create_account;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.accountservice.dto.AccountRequest;
import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.exception.JsonMappingException;
import faang.school.accountservice.service.AccountOwnerService;
import faang.school.accountservice.service.request.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckAccountsQuantityTest {

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private AccountOwnerService accountOwnerService;

    @Mock
    private RequestService requestService;

    @InjectMocks
    private CheckAccountsQuantityHandler checkAccountsQuantity;

    @BeforeEach
    public void setUp() throws Exception {
        Field maxAccountsQuantityField =
                CheckAccountsQuantityHandler.class.getDeclaredField("maxAccountsQuantity");
        maxAccountsQuantityField.setAccessible(true);
        maxAccountsQuantityField.set(checkAccountsQuantity, 2);
    }

    @Test
    public void executeThrowsIllegalStateExceptionTest() throws JsonProcessingException {
        long ownerId = 1L;
        OwnerType ownerType = OwnerType.USER;

        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        AccountRequest accountRequest = AccountRequest.builder()
                .ownerId(ownerId)
                .ownerType(OwnerType.USER)
                .build();

        Request request = Request.builder()
                .requestStatus(RequestStatus.AWAITING)
                .context(objectMapper.writeValueAsString(accountRequest))
                .requestTasks(List.of(requestTask1, requestTask2))
                .build();

        AccountOwner accountOwner = AccountOwner.builder()
                .id(ownerId)
                .accounts(List.of(Account.builder().build(), Account.builder().build()))
                .build();

        when(accountOwnerService.findOwner(ownerId, ownerType)).thenReturn(accountOwner);

        RequestTask updatedTask1 = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

        RequestTask updatedTask2 = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

        assertThrows(IllegalStateException.class, () -> checkAccountsQuantity.execute(request));
        assertEquals(RequestStatus.DONE, request.getRequestStatus());
        assertEquals(RequestTaskStatus.DONE, updatedTask1.getStatus());
        assertEquals(RequestTaskStatus.DONE, updatedTask2.getStatus());
    }

    @Test
    public void executeThrowsOptimisticLockingFailureExceptionTest() throws JsonProcessingException {
        long ownerId = 1L;
        OwnerType ownerType = OwnerType.USER;

        AccountOwner owner = AccountOwner.builder()
                .id(ownerId)
                .accounts(new ArrayList<>())
                .build();

        AccountRequest accountRequest = AccountRequest.builder()
                .ownerId(ownerId)
                .ownerType(ownerType)
                .build();

        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                .build();

        Request request = Request.builder()
                .context(objectMapper.writeValueAsString(accountRequest))
                .requestTasks(List.of(requestTask1))
                .build();

        when(accountOwnerService.findOwner(ownerId, ownerType)).thenReturn(owner);
        doThrow(OptimisticLockingFailureException.class).when(requestService).updateRequest(request);

        assertThrows(OptimisticLockingFailureException.class, () -> checkAccountsQuantity.execute(request));

        verify(requestService, times(1)).updateRequest(request);
        assertEquals(RequestStatus.PROCESSING, request.getRequestStatus());
        assertEquals(RequestTaskStatus.DONE, request.getRequestTasks().get(0).getStatus());
    }


    @Test
    public void executeThrowsJsonProcessingExceptionTest() throws JsonProcessingException {
        long ownerId = 1L;

        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        AccountRequest accountRequest = AccountRequest.builder()
                .ownerId(ownerId)
                .ownerType(OwnerType.USER)
                .build();

        Request request = Request.builder()
                .requestStatus(RequestStatus.AWAITING)
                .requestTasks(List.of(requestTask1, requestTask2))
                .context(objectMapper.writeValueAsString(accountRequest))
                .build();

        when(objectMapper.readValue(request.getContext(), AccountRequest.class)).
                thenThrow(JsonProcessingException.class);

        assertThrows(JsonMappingException.class, () -> checkAccountsQuantity.execute(request));
    }

    @Test
    public void executeTest() throws JsonProcessingException {
        long ownerId = 1L;
        OwnerType ownerType = OwnerType.USER;

        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        AccountRequest accountRequest = AccountRequest.builder()
                .ownerId(ownerId)
                .ownerType(OwnerType.USER)
                .build();

        Request request = Request.builder()
                .requestStatus(RequestStatus.AWAITING)
                .context(objectMapper.writeValueAsString(accountRequest))
                .requestTasks(List.of(requestTask1, requestTask2))
                .build();

        AccountOwner accountOwner = AccountOwner.builder()
                .id(ownerId)
                .accounts(List.of(Account.builder().build()))
                .build();

        when(accountOwnerService.findOwner(ownerId, ownerType)).thenReturn(accountOwner);

        checkAccountsQuantity.execute(request);

        RequestTask updatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

        RequestTask notUpdatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

        verify(requestService).updateRequest(request);
        assertEquals(RequestStatus.PROCESSING, request.getRequestStatus());
        assertEquals(RequestTaskStatus.DONE, updatedTask.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, notUpdatedTask.getStatus());
    }

        @Test
    public void rollbackTest() {
            RequestTask requestTask1 = RequestTask.builder()
                    .status(RequestTaskStatus.AWAITING)
                    .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                    .build();

            RequestTask requestTask2 = RequestTask.builder()
                    .status(RequestTaskStatus.AWAITING)
                    .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                    .build();

            Request request = Request.builder()
                    .requestStatus(RequestStatus.AWAITING)
                    .requestTasks(List.of(requestTask1, requestTask2))
                    .build();

            checkAccountsQuantity.rollback(request);

            RequestTask updatedTask = request.getRequestTasks().stream()
                    .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

            RequestTask notUpdatedTask = request.getRequestTasks().stream()
                    .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

            verify(requestService).updateRequest(request);
            assertEquals(RequestStatus.AWAITING, request.getRequestStatus());
            assertEquals(RequestTaskStatus.AWAITING, updatedTask.getStatus());
            assertEquals(RequestTaskStatus.AWAITING, notUpdatedTask.getStatus());
        }
}