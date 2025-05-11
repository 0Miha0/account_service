package faang.school.accountservice.service.request_task.handler.impl.create_account;

import faang.school.accountservice.entity.Account;
import faang.school.accountservice.entity.Request;
import faang.school.accountservice.entity.RequestTask;
import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request_task.RequestTaskStatus;
import faang.school.accountservice.enums.request_task.RequestTaskType;
import faang.school.accountservice.event.CreateAccountEvent;
import faang.school.accountservice.publisher.CreateAccountPublisher;
import faang.school.accountservice.repository.AccountRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SendCreateAccountNotificationHandler implements RequestTaskHandler {

    private static final Long HANDLER_ID = 5L;

    private final CreateAccountPublisher publisher;
    private final RequestService requestService;
    private final AccountRepository accountRepository;

    private final CheckAccountsQuantityHandler checkAccountsQuantity;
    private final CreateAccountHandler createAccount;
    private final CreateBalanceAndBalanceAuditHandler balanceAudit;

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Override
    public void execute(Request request) {
        RequestTask requestTask = getPerticularRequestTask(request);
        if (requestTask.getStatus() == RequestTaskStatus.DONE) {
            log.warn("Request task with id: {} already completed.", requestTask.getId());
            return;
        }

        try {
            Long accountId = Long.valueOf(request.getContext());
            Account account = accountRepository.findById(accountId).orElseThrow(() ->
                    new EntityNotFoundException("Account with id " + accountId + " not found"));

            CreateAccountEvent event = CreateAccountEvent.builder()
                    .ownerId(account.getOwner().getOwnerId())
                    .ownerType(account.getOwner().getOwnerType().name())
                    .accountType(account.getType().name())
                    .currency(account.getCurrency().name())
                    .build();

            request.setContext(null);
            request.setRequestStatus(RequestStatus.DONE);
            setRequestTaskStatus(request, RequestTaskStatus.DONE);

            requestService.updateRequest(request);
            publisher.publish(event);
            log.info("Finished processing request task with type: {}",
                    RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION);
            log.info("Successfully opened account with number: {}", account.getAccountNumber());

        } catch (Exception e) {
            log.error("Unexpected error occurred during execution request with id: {}. " +
                    "Executing rollback.", request.getIdempotentToken(), e);
            rollback(request);
            throw e;
        }
    }

    @Transactional
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
        request.setRequestStatus(RequestStatus.AWAITING);
        setRequestTasksStatus(request, RequestTaskStatus.AWAITING);
        requestService.updateRequest(request);

        balanceAudit.rollback(request);
        createAccount.rollback(request);
        checkAccountsQuantity.rollback(request);
        log.info("Request task with type: {} rollback", RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION);
    }

    private void setRequestTaskStatus(Request request, RequestTaskStatus requestTaskStatus) {
        request.getRequestTasks().stream()
                .filter(requestTask -> requestTask.getHandler().
                        equals(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION))
                .forEach(requestTask -> requestTask.setStatus(requestTaskStatus));
    }

    private void setRequestTasksStatus(Request request, RequestTaskStatus status) {
        request.getRequestTasks()
                .forEach(requestTask -> requestTask.setStatus(status));
    }

    private RequestTask getPerticularRequestTask(Request request) {
        return request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION))
                .findFirst().orElseThrow(
                        () -> new EntityNotFoundException("No request task found for type: %s".
                                formatted(RequestTaskType.SEND_CREATE_ACCOUNT_NOTIFICATION)));
    }
}