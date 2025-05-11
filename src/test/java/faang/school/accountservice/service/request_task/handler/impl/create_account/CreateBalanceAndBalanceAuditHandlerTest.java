package faang.school.accountservice.service.request_task.handler.impl.create_account;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.accountservice.dto.BalanceDto;
import faang.school.accountservice.dto.rollbeck.RollbackTaskCrateBalanceDto;
import faang.school.accountservice.entity.BalanceAudit;
import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.exception.JsonMappingException;
import faang.school.accountservice.repository.AccountRepository;
import faang.school.accountservice.service.BalanceAuditService;
import faang.school.accountservice.service.BalanceService;
import faang.school.accountservice.service.request.RequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBalanceAndBalanceAuditHandlerTest {

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private BalanceService balanceService;

    @Mock
    private RequestService requestService;

    @Mock
    private BalanceAuditService balanceAuditService;

    @Mock
    private CheckAccountsQuantityHandler checkAccountsQuantity;

    @Mock
    private CreateAccountHandler createAccount;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CreateBalanceAndBalanceAuditHandler createBalanceAndBalanceAuditHandler;

    @Test
    public void executeTest() throws JsonProcessingException {
        Long accountId = 2L;
        String accountNumber = "4222000000000001";

        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .id(11L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        BalanceAudit balanceAudit = BalanceAudit.builder()
                .id(1L)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(accountNumber)
                .type(AccountType.DEBIT)
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .balanceAudits(List.of(balanceAudit))
                .build();

        Request request = Request.builder()
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context(accountId.toString())
                .build();

        BalanceDto balanceDto = BalanceDto.builder()
                .id(2L)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(balanceService.createBalance(account)).thenReturn(balanceDto);

        createBalanceAndBalanceAuditHandler.execute(request);

        verify(requestService).updateRequest(request);

        RollbackTaskCrateBalanceDto rollbackTaskCrateBalanceDto =
                objectMapper.readValue(requestTask1.getRollbackContext(), RollbackTaskCrateBalanceDto.class);

        assertEquals(RequestTaskStatus.DONE, requestTask1.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, requestTask2.getStatus());
        assertEquals(balanceDto.getId(), rollbackTaskCrateBalanceDto.getBalanceId());
        assertTrue(rollbackTaskCrateBalanceDto.getBalanceAuditIds().contains(balanceAudit.getId()));
    }

    @Test
    public void executeOptimisticLockingFailureExceptionTest() {
        Long accountId = 2L;
        String accountNumber = "4222000000000001";

        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .id(11L)
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        BalanceAudit balanceAudit = BalanceAudit.builder()
                .id(1L)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(accountNumber)
                .type(AccountType.DEBIT)
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .balanceAudits(List.of(balanceAudit))
                .build();

        Request request = Request.builder()
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context(accountId.toString())
                .build();

        BalanceDto balanceDto = BalanceDto.builder()
                .id(2L)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(balanceService.createBalance(account)).thenReturn(balanceDto);
        doThrow(OptimisticLockingFailureException.class).
                when(requestService).updateRequest(any(Request.class));

        assertThrows(OptimisticLockingFailureException.class,
                () -> createBalanceAndBalanceAuditHandler.execute(request));
        System.out.println(request.getRequestTasks());

        verify(requestService).updateRequest(request);
        verify(balanceService).deleteBalance(balanceDto.getId());
        verify(balanceAuditService).deleteAudit(balanceAudit.getId());
        verify(checkAccountsQuantity).rollback(request);
        verify(createAccount).rollback(request);

        RequestTask updatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask1.getHandler())).findFirst().get();

        RequestTask notUpdatedTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(requestTask2.getHandler())).findFirst().get();

        assertEquals(RequestTaskStatus.AWAITING, updatedTask.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, notUpdatedTask.getStatus());
    }

    @Test
    public void executeJsonMappingExceptionTest() throws JsonProcessingException {
        Long accountId = 2L;
        String accountNumber = "4222000000000001";

        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.AWAITING)
                .handler(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT)
                .build();

        RequestTask requestTask2 = RequestTask.builder()
                .id(11L)
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_ACCOUNT)
                .build();

        BalanceAudit balanceAudit = BalanceAudit.builder()
                .id(1L)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(accountNumber)
                .type(AccountType.DEBIT)
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .balanceAudits(List.of(balanceAudit))
                .build();

        Request request = Request.builder()
                .requestTasks(new ArrayList<>(List.of(requestTask1, requestTask2)))
                .context(accountId.toString())
                .build();

        BalanceDto balanceDto = BalanceDto.builder()
                .id(2L)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(balanceService.createBalance(account)).thenReturn(balanceDto);
        when(objectMapper.writeValueAsString(any(RollbackTaskCrateBalanceDto.class)))
                .thenThrow(JsonProcessingException.class);

        assertThrows(JsonMappingException.class,
                () -> createBalanceAndBalanceAuditHandler.execute(request));

        verify(checkAccountsQuantity).rollback(request);
        verify(createAccount).rollback(request);
    }

    @Test
    public void rollbackTest() {
        Long accountId = 2L;

        RequestTask requestTask1 = RequestTask.builder()
                .id(10L)
                .status(RequestTaskStatus.DONE)
                .handler(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT)
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

        createBalanceAndBalanceAuditHandler.rollback(request);

        verify(checkAccountsQuantity).rollback(request);
        verify(createAccount).rollback(request);

        assertEquals(RequestTaskStatus.AWAITING, requestTask1.getStatus());
        assertEquals(RequestTaskStatus.AWAITING, requestTask2.getStatus());
        assertNull(requestTask1.getRollbackContext());
    }
}