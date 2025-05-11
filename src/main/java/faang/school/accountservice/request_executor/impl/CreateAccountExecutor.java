package faang.school.accountservice.request_executor.impl;

import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.request_executor.RequestProcessExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class CreateAccountExecutor implements RequestProcessExecutor {

    private final Executor createAccountExecutor;

    public CreateAccountExecutor(@Qualifier("createAccountThreadPool") Executor createAccountExecutor) {
        this.createAccountExecutor = createAccountExecutor;
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.CREATE_ACCOUNT;
    }

    @Override
    public Executor getThreadPoolExecutor() {
        return createAccountExecutor;
    }
}
