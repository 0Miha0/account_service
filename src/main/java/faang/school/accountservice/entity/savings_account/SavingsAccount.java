package faang.school.accountservice.entity.savings_account;

import faang.school.accountservice.entity.Account;
import faang.school.accountservice.entity.tariff.Tariff;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "savings_account")
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true, nullable = false)
    private Account account;

    @Column(name = "last_interest_date")
    private LocalDateTime lastInterestDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_tariff_id", nullable = false)
    private Tariff tariff;

    @Builder.Default
    @OneToMany(mappedBy = "savingsAccount", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SavingsAccountTariffChangelog> tariffChangelogs = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public void changeTariff(Tariff tariff) {
        this.setTariff(tariff);
        SavingsAccountTariffChangelog tariffChangelog = new SavingsAccountTariffChangelog();
        tariffChangelog.setTariff(tariff);
        tariffChangelog.setSavingsAccount(this);
        this.tariffChangelogs.add(tariffChangelog);
    }
}
