package faang.school.accountservice.service.request_task.handler.impl.create_account;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.event.CreateAccountEvent;
import faang.school.accountservice.publisher.CreateAccountPublisher;
import faang.school.accountservice.repository.AccountRepository;
import faang.school.accountservice.service.request.RequestService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendCreateAccountNotificationHandlerTest {

    @Mock
    private CreateAccountPublisher publisher;

    @Mock
    private RequestService requestService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CheckAccountsQuantityHandler checkAccountsQuantity;

    @Mock
    private CreateAccountHandler createAccount;

    @Mock
    private CreateBalanceAndBalanceAuditHandler balanceAudit;

    @InjectMocks
    private SendCreateAccountNotificationHandler sendCreateAccountNotificationHandler;

    @Test
    public void executeTest() {
        Long accountId = 2L;
        Long ownerId = 5L;

        AccountOwner accountOwner = AccountOwner.builder()
                .id(ownerId)
                .ownerType(OwnerType.USER)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .owner(accountOwner)
                .type(AccountType.DEBIT)
                .currency(Currency.USD)
                .build();

        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .id(11L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        Request request = Request.builder()
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context(accountId.toString())
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        sendCreateAccountNotificationHandler.execute(request);

        verify(requestService).updateRequest(request);
        verify(publisher).publish(any(CreateAccountEvent.class));

        RequestTask updatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

        RequestTask notUpdatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

        assertNull(request.getContext());
        assertEquals(RequestStatus.DONE, request.getRequestStatus());
        assertEquals(RequestTaskStatus.DONE, updatedTask.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, notUpdatedTask.getStatus());
    }

    @Test
    public void executeThrowsOptimisticLockingFailureExceptionTest() {
        AtomicInteger callCount = new AtomicInteger(0);
        Long accountId = 2L;
        Long ownerId = 5L;

        AccountOwner accountOwner = AccountOwner.builder()
                .id(ownerId)
                .ownerType(OwnerType.USER)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .owner(accountOwner)
                .type(AccountType.DEBIT)
                .currency(Currency.USD)
                .build();

        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .id(11L)
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        Request request = Request.builder()
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context(accountId.toString())
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        doAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0) {
                throw new OptimisticLockingFailureException("Simulated exception on first call");
            }
            return null;
        }).when(requestService).updateRequest(any(Request.class));

        assertThrows(OptimisticLockingFailureException.class,
                () -> sendCreateAccountNotificationHandler.execute(request));

        verify(accountRepository).findById(accountId);
        verify(requestService, times(2)).updateRequest(request);
        verify(publisher, times(0)).publish(any(CreateAccountEvent.class));
        verify(balanceAudit).rollback(request);
        verify(createAccount).rollback(request);
        verify(checkAccountsQuantity).rollback(request);

        RequestTask updatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

        RequestTask notUpdatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

        assertEquals(RequestStatus.AWAITING, request.getRequestStatus());
        assertEquals(RequestTaskStatus.AWAITING, updatedTask.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, notUpdatedTask.getStatus());
    }

    @Test
    public void executeThrowsEntityNotFoundExceptionTest() {
        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .id(11L)
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        Request request = Request.builder()
                .context("2")
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .build();
        when(accountRepository.findById(2L)).thenThrow(EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class, () -> sendCreateAccountNotificationHandler.execute(request));
    }

    @Test
    public void rollback() {
        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .id(11L)
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        Request request = Request.builder()
                .context("2")
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .build();

        sendCreateAccountNotificationHandler.rollback(request);

        verify(requestService).updateRequest(request);
        verify(balanceAudit).rollback(request);
        verify(createAccount).rollback(request);
        verify(checkAccountsQuantity).rollback(request);

        RequestTask updatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

        RequestTask notUpdatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

        assertEquals(RequestStatus.AWAITING, request.getRequestStatus());
        assertEquals(RequestTaskStatus.AWAITING, updatedTask.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, notUpdatedTask.getStatus());
    }
}