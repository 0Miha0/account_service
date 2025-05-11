package faang.school.accountservice.service.request;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.service.request_task.handler.RequestTaskHandler;
import faang.school.accountservice.service.request_task.handler.impl.create_account.CheckAccountsQuantityHandler;
import faang.school.accountservice.service.request_task.handler.impl.create_account.CreateAccountHandler;
import faang.school.accountservice.service.request_task.handler.impl.create_account.CreateBalanceAndBalanceAuditHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestExecutorServiceTest {

    @Mock
    private CheckAccountsQuantityHandler checkAccountsQuantity;

    @Mock
    private CreateAccountHandler createAccount;

    @Mock
    private CreateBalanceAndBalanceAuditHandler balanceAudit;

    private RequestExecutorService requestExecutorService;

    @BeforeEach
    public void setUp() {
        List<RequestTaskHandler> handlers =
                new ArrayList<>(List.of(createAccount, checkAccountsQuantity, balanceAudit));
        requestExecutorService = new RequestExecutorService(handlers);
    }

    @Test
    public void executeRequestTest() {
        Request request = Request.builder()
                .requestType(RequestType.CREATE_ACCOUNT)
                .build();

        when(checkAccountsQuantity.getHandlerId()).thenReturn(1L);
        when(createAccount.getHandlerId()).thenReturn(2L);
        when(balanceAudit.getHandlerId()).thenReturn(10L);
        doNothing().when(checkAccountsQuantity).execute(request);
        doNothing().when(createAccount).execute(request);

        requestExecutorService.executeRequest(request);

        verify(checkAccountsQuantity, times(1)).execute(request);
        verify(createAccount, times(1)).execute(request);
        verify(balanceAudit, times(0)).execute(request);
    }
}
