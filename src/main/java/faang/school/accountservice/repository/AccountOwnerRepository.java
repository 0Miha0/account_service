package faang.school.accountservice.repository;

import faang.school.accountservice.entity.AccountOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountOwnerRepository extends JpaRepository<AccountOwner, Long> {

}
