package faang.school.accountservice.controller.request;

import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.service.request.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequestMapping("api/v1/requests")
@RestController
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/{requestId}/statuses")
    public RequestStatus getRequestStatus(@PathVariable UUID requestId) {
        return requestService.getRequestStatus(requestId);
    }
}
