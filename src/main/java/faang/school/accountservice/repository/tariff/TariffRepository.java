package faang.school.accountservice.repository.tariff;

import faang.school.accountservice.entity.tariff.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    Optional<Tariff> findByName(String name);
}
