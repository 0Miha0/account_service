package faang.school.accountservice.controller.request;

import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.service.request.RequestService;
import faang.school.accountservice.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestControllerMockMvcTest extends BaseContextTest {

    @MockBean
    private RequestService requestService;

    @Test
    public void getRequestStatusTest() throws Exception {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        long userId = 1L;

        when(requestService.getRequestStatus(id)).thenReturn(RequestStatus.DONE);

        mockMvc.perform(get("/api/v1/requests/{requestId}/statuses", id)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("x-user-id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(RequestStatus.DONE.toString()));
    }
}