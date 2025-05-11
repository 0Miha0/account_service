package faang.school.accountservice.repository;

import faang.school.accountservice.entity.FreeAccountNumber;
import faang.school.accountservice.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FreeAccountNumbersRepository extends JpaRepository<FreeAccountNumber, Long> {

    @Query(nativeQuery = true,
            value = """
                        DELETE FROM free_account_numbers fan
                        WHERE fan.account_type = :accountType AND fan.account_number = (
                            SELECT account_number
                            FROM free_account_numbers
                            WHERE account_type = :accountType
                            LIMIT 1
                        )
                        RETURNING fan.account_number, fan.account_type
                    """)
    FreeAccountNumber retrieveFreeAccountNumber(@Param("accountType") String accountType);

    long countByAccountType(AccountType accountType);
}