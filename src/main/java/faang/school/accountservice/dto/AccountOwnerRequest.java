package faang.school.accountservice.dto;

import faang.school.accountservice.enums.OwnerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountOwnerRequest {
    private Long ownerId;
    private OwnerType ownerType;
}
