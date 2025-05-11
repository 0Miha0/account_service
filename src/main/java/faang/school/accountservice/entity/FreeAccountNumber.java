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

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "free_account_numbers")
public class FreeAccountNumber {

    @Column(name = "account_type", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Id
    @Column(name = "account_number", length = 20, nullable = false, unique = true)
    private String accountNumber;
}