package faang.school.accountservice.entity;

import faang.school.accountservice.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account_numbers_sequence")
public class AccountNumberSequence {

    @Id
    @Column(name = "account_type", length = 32, nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(name = "current_value", nullable = false)
    private Long currentSequenceValue;
}