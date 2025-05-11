package faang.school.accountservice.service.request_task.handler;

import faang.school.accountservice.entity.Request;

public interface RequestTaskHandler {

    void execute(Request request);

    long getHandlerId();

    void rollback(Request request);
}