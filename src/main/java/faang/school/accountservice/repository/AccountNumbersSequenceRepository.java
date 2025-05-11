package faang.school.accountservice.repository;

import faang.school.accountservice.entity.AccountNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountNumbersSequenceRepository extends JpaRepository<AccountNumberSequence, Long> {

    @Query(nativeQuery = true, value = """
        UPDATE account_numbers_sequence
        SET current_value = current_value + :batchSize
        WHERE account_type = :accountType
        RETURNING account_type, current_value"""
    )
    AccountNumberSequence incrementCounter(@Param("accountType") String accountType, @Param("batchSize") long batchSize);
}