package faang.school.accountservice.dto;

import faang.school.accountservice.enums.OwnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOwnerWithAccountsResponse {
    private Long id;
    private Long ownerId;
    private OwnerType ownerType;
    private LocalDateTime createdAt;
    private List<AccountResponse> accounts;
}
