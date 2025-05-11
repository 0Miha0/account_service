package faang.school.accountservice.request_executor;

import faang.school.accountservice.enums.request.RequestType;

import java.util.concurrent.Executor;

public interface RequestProcessExecutor {

    RequestType getRequestType();

    Executor getThreadPoolExecutor();
}
