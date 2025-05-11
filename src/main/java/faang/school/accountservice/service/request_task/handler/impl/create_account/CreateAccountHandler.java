package faang.school.accountservice.service.request_task.handler.impl.create_account;

import faang.school.accountservice.entity.Account;
import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.service.AccountService;
import faang.school.accountservice.service.request.RequestService;
import faang.school.accountservice.service.request_task.handler.RequestTaskHandler;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAccountHandler implements RequestTaskHandler {

    private static final Long HANDLER_ID = 2L;

    private final AccountService accountService;
    private final RequestService requestService;

    private final CheckAccountsQuantityHandler checkAccountsQuantity;

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Override
    public void execute(Request request) {
        RequestTask requestTask = getPerticularRequestTask(request);
        if (requestTask.getStatus()== RequestTaskStatus.DONE) {
            log.warn("Request task with id: {} already completed.", requestTask.getId());
            return;
        }
        try {
            Account account = accountService.createAccount(request);
            request.setContext(account.getId().toString());
            setRequestTaskStatusAndContext(request, RequestTaskStatus.DONE,
                    account.getId().toString());
            requestService.updateRequest(request);
            log.info("Finished processing request task with type: {}",
                    RequestTaskType.WRITE_INTO_ACCOUNT);

        } catch (Exception e) {
            log.error("Unexpected error occurred during execution request with id: {}. " +
                    "Executing rollback.", request.getIdempotentToken(), e);
            rollback(request);
            throw e;
        }
    }

    @Recover
    public void recover(OptimisticLockingFailureException e, Request request) {
        log.error("Optimistic locking failed after 3 retries for request with id: {}. " +
                "Executing rollback.", request.getIdempotentToken(), e);
        rollback(request);
    }

    @Override
    public long getHandlerId() {
        return HANDLER_ID;
    }

    @Transactional
    @Override
    public void rollback(Request request) {
        RequestTask requestTask = request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(RequestTaskType.WRITE_INTO_ACCOUNT))
                .findFirst().orElseThrow(() -> new EntityNotFoundException("No request task found"));

        if (requestTask.getRollbackContext() != null) {
            accountService.deleteAccount(Long.getLong(requestTask.getRollbackContext()));
        }

        List<RequestTask> tasks = request.getRequestTasks().stream()
                .filter(task -> task.getStatus() == RequestTaskStatus.DONE).toList();
        if (!tasks.isEmpty()) {
            setRequestTasksStatus(request, RequestTaskStatus.AWAITING);
            checkAccountsQuantity.rollback(request);
        }
        log.info("Request task with type: {}, id: {} rollback",
                RequestTaskType.WRITE_INTO_ACCOUNT, requestTask.getId());
    }

    private void setRequestTasksStatus(Request request, RequestTaskStatus status) {
        request.getRequestTasks()
                .forEach(requestTask -> requestTask.setStatus(status));
    }

    private void setRequestTaskStatusAndContext(
            Request request, RequestTaskStatus requestTaskStatus, String taskContext) {
        request.getRequestTasks().stream()
                .filter(requestTask -> requestTask.getHandler().
                        equals(RequestTaskType.WRITE_INTO_ACCOUNT))
                .forEach(requestTask -> {
                    requestTask.setStatus(requestTaskStatus);
                    requestTask.setRollbackContext(taskContext);
                });
    }

    private RequestTask getPerticularRequestTask(Request request) {
        return request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(RequestTaskType.WRITE_INTO_ACCOUNT))
                .findFirst().orElseThrow(
                        () -> new EntityNotFoundException("No request task found for type: %s".
                                formatted(RequestTaskType.WRITE_INTO_ACCOUNT)));
    }
}
