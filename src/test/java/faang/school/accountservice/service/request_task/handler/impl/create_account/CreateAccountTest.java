package faang.school.accountservice.service.request_task.handler.impl.create_account;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.service.AccountService;
import faang.school.accountservice.service.request.RequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountTest {

    @Mock
    private AccountService accountService;

    @Mock
    private RequestService requestService;

    @Mock
    private CheckAccountsQuantityHandler checkAccountsQuantity;

    @InjectMocks
    private CreateAccountHandler createAccount;

    @Test
    public void executeTest() {
        long accountId = 2L;
        String accountNumber = "4222000000000001";

        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION)
                .build();

        Request request = Request.builder()
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context(String.valueOf(accountId))
                .build();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(accountNumber)
                .type(AccountType.DEBIT)
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.createAccount(request)).thenReturn(account);

        createAccount.execute(request);

        RequestTask updatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

        RequestTask notUpdatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

        verify(requestService).updateRequest(request);

        assertEquals(account.getId().toString(), request.getContext());
        assertEquals(RequestTaskStatus.DONE, updatedTask.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, notUpdatedTask.getStatus());
        assertEquals(String.valueOf(accountId), updatedTask.getRollbackContext());
    }

    @Test
    public void executeThrowsOptimisticLockingFailureExceptionTest() {
        long accountId = 2L;
        String accountNumber = "4222000000000001";

        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                .build();

        Request request = Request.builder()
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context(String.valueOf(accountId))
                .build();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(accountNumber)
                .type(AccountType.DEBIT)
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.createAccount(request)).thenReturn(account);
        doThrow(OptimisticLockingFailureException.class).when(requestService).updateRequest(any());

        assertThrows(OptimisticLockingFailureException.class, () -> createAccount.execute(request));

        verify(checkAccountsQuantity).rollback(request);
        verify(accountService).createAccount(request);
    }

    @Test
    public void rollbackTest() {
        RequestTask requestTask1 = RequestTask.builder()
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .rollbackContext("1")
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.CHECK_ACCOUNTS_QUANTITY)
                .build();

        Request request = Request.builder()
                .requestStatus(RequestStatus.PROCESSING)
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .build();

        createAccount.rollback(request);

        verify(accountService).deleteAccount(Long.getLong(requestTask1.getRollbackContext()));
        verify(checkAccountsQuantity).rollback(request);
        assertEquals(RequestTaskStatus.AWAITING, requestTask1.getStatus());
    }
}