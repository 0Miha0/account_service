package faang.school.accountservice.repository.savings_account;

import faang.school.accountservice.entity.savings_account.SavingsAccountTariffChangelog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsAccountTariffChangelogRepository extends JpaRepository<SavingsAccountTariffChangelog, Long> {

    List<SavingsAccountTariffChangelog> findBySavingsAccountId(long savingsAccountId);
}
