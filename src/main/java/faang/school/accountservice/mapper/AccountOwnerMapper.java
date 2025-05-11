package faang.school.accountservice.mapper;

import faang.school.accountservice.dto.AccountOwnerResponse;
import faang.school.accountservice.dto.AccountOwnerWithAccountsResponse;
import faang.school.accountservice.entity.AccountOwner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountOwnerMapper {

    AccountOwnerResponse toDto(AccountOwner owner);

    @Mapping(source = "accounts", target = "accounts")
    AccountOwnerWithAccountsResponse toOwnerWithAccountsDto(AccountOwner owner);
}
