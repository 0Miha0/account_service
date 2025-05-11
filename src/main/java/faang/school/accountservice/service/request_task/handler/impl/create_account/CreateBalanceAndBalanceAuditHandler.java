package faang.school.accountservice.service.request_task.handler.impl.create_account;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.accountservice.dto.BalanceDto;
import faang.school.accountservice.dto.rollbeck.RollbackTaskCrateBalanceDto;
import faang.school.accountservice.entity.Account;
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
public class CreateBalanceAndBalanceAuditHandler implements RequestTaskHandler {

    private static final Long HANDLER_ID = 3L;

    private final BalanceService balanceService;
    private final ObjectMapper objectMapper;
    private final RequestService requestService;
    private final BalanceAuditService balanceAuditService;
    private final AccountRepository accountRepository;

    private final CheckAccountsQuantityHandler checkAccountsQuantity;
    private final CreateAccountHandler createAccount;

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
            Long accountId = Long.valueOf(request.getContext());
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Account with Id: " +
                            accountId + " not found"));
            BalanceDto balanceDto = balanceService.createBalance(account);
            List<BalanceAudit> audits = account.getBalanceAudits();
            List<Long> auditsIds = audits.stream()
                    .map(BalanceAudit::getId).toList();

            RollbackTaskCrateBalanceDto context = RollbackTaskCrateBalanceDto.builder()
                    .balanceId(balanceDto.getId())
                    .balanceAuditIds(auditsIds)
                    .build();

            String rollbackContext = mapRollbackTaskDtoToString(context);
            setRequestTaskStatusAndContext(request, RequestTaskStatus.DONE, rollbackContext);
            requestService.updateRequest(request);
            log.info("Finished processing request task with type: {}, id: {}",
                    RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT, request.getIdempotentToken());

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
                .filter(task -> task.getHandler().equals(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT))
                .findFirst().orElseThrow(() -> new EntityNotFoundException("No request task found"));

        if (requestTask.getRollbackContext() != null) {
            RollbackTaskCrateBalanceDto dto = mapRollbackTaskDto(requestTask.getRollbackContext());
            balanceService.deleteBalance(dto.getBalanceId());
            dto.getBalanceAuditIds().forEach(balanceAuditService::deleteAudit);
        }

        List<RequestTask> tasks = request.getRequestTasks().stream()
                .filter(task -> task.getStatus() == RequestTaskStatus.DONE).toList();

        if (!tasks.isEmpty()) {
            setRequestTasksStatus(request, RequestTaskStatus.AWAITING);
            checkAccountsQuantity.rollback(request);
            createAccount.rollback(request);
        }
        log.info("Request task with id: {}, type: {} rollback",
                request.getIdempotentToken(), RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT);
    }

    private String mapRollbackTaskDtoToString(RollbackTaskCrateBalanceDto dto) {
        String rollbackContext;
        try {
            rollbackContext = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new JsonMappingException(e.getMessage());
        }
        return rollbackContext;
    }

    private RollbackTaskCrateBalanceDto mapRollbackTaskDto(String taskContext) {
        RollbackTaskCrateBalanceDto dto;
        try {
            dto = objectMapper.readValue(taskContext, RollbackTaskCrateBalanceDto.class);
        } catch (JsonProcessingException e) {
            throw new JsonMappingException(e.getMessage());
        }
        return dto;
    }

    private void setRequestTasksStatus(Request request, RequestTaskStatus status) {
        request.getRequestTasks()
                .forEach(requestTask -> requestTask.setStatus(status));
    }

    private void setRequestTaskStatusAndContext(
            Request request, RequestTaskStatus requestTaskStatus, String taskContext) {
        request.getRequestTasks().stream()
                .filter(requestTask -> requestTask.getHandler().
                        equals(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT))
                .forEach(requestTask -> {
                    requestTask.setStatus(requestTaskStatus);
                    requestTask.setRollbackContext(taskContext);
                });
    }

    private RequestTask getPerticularRequestTask(Request request) {
        return request.getRequestTasks().stream()
                .filter(task -> task.getHandler().equals(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT))
                .findFirst().orElseThrow(
                        () -> new EntityNotFoundException("No request task found for type: %s".
                                formatted(RequestTaskType.WRITE_INTO_BALANCE_BALANCE_AUDIT)));
    }
}
