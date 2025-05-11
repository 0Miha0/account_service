package faang.school.accountservice.dto;

import faang.school.accountservice.enums.OwnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOwnerResponse {
    private Long id;
    private Long ownerId;
    private OwnerType ownerType;
    private LocalDateTime createdAt;
}
