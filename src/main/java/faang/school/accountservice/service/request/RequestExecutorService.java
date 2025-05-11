package faang.school.accountservice.service.request;

import faang.school.accountservice.entity.Request;
import faang.school.accountservice.enums.request.RequestType;
import faang.school.accountservice.service.request_task.handler.RequestTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestExecutorService {

    private final List<RequestTaskHandler> requestTaskHandlers;

    public void executeRequest(Request request) {
        List<Long> handlersIds = getHandlersIdsByRequestType(request.getRequestType());

        requestTaskHandlers.stream()
                .filter(handler -> handlersIds.contains(handler.getHandlerId()))
                .sorted(Comparator.comparing(RequestTaskHandler::getHandlerId))
                .forEach(requestTaskHandler -> requestTaskHandler.execute(request));
    }

    private List<Long> getHandlersIdsByRequestType(RequestType requestType) {
        return switch (requestType) {
            case CREATE_ACCOUNT -> new ArrayList<>(List.of(1L, 2L, 3L, 5L));
        };
    }
}