package faang.school.accountservice.service.tariff;

import faang.school.accountservice.dto.tariff.TariffCreateDto;
import faang.school.accountservice.dto.tariff.TariffResponse;
import faang.school.accountservice.entity.tariff.Tariff;
import faang.school.accountservice.entity.tariff.TariffRateChangelog;
import faang.school.accountservice.exception.UniqueConstraintException;
import faang.school.accountservice.mapper.tariff.TariffMapper;
import faang.school.accountservice.repository.tariff.TariffRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffService {

    private static final String TARIFF_NAME_UNIQUE_CONSTRAINT = "tariff_name_key";

    private final TariffRepository tariffRepository;
    private final TariffMapper tariffMapper;

    @Transactional
    public TariffResponse createTariff(TariffCreateDto creationDto) {
        String tariffName = creationDto.getName();
        BigDecimal tariffRate = creationDto.getRate();
        log.info("Received request to create a new tariff with name '{}' and initial rate={}", tariffName, tariffRate);

        TariffResponse response = null;
        try {
            Tariff tariff = tariffMapper.toEntity(creationDto);
            TariffRateChangelog rateChangelog = buildTariffRateChangelog(tariff, tariffRate);
            tariff.setRateChangelogs(List.of(rateChangelog));
            tariff = tariffRepository.save(tariff);
            log.info("A new tariff named '{}' with initial rate={} was successfully created.", tariffName, tariffRate);
            response = tariffMapper.toResponse(tariff);
        } catch (DataIntegrityViolationException ex) {
            handleDataIntegrityViolationException(ex, tariffName);
        }
        return response;
    }

    @Retryable(
            retryFor = OptimisticLockException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Transactional
    public TariffResponse updateTariffRate(long tariffId, BigDecimal newRate) {
        log.info("Received request to update the rate of the tariff with ID={}. New rate={}", tariffId, newRate);

        Tariff tariff = getTariffById(tariffId);
        tariff.setCurrentRate(newRate);
        TariffRateChangelog tariffRateChangelog = buildTariffRateChangelog(tariff, newRate);
        tariff.getRateChangelogs().add(tariffRateChangelog);
        tariff = tariffRepository.save(tariff);

        TariffResponse tariffResponse = tariffMapper.toResponse(tariff);
        log.info("The tariff with ID={} was updated: {}", tariffId, tariffResponse);
        return tariffResponse;
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> getAllTariffs() {
        return tariffRepository.findAll().stream()
                .map(tariffMapper::toResponse)
                .toList();
    }

    @Recover
    public void recover(OptimisticLockException ex, long tariffId, BigDecimal newRate) {
        throw new RuntimeException("Retries exhausted. Could not set new rate=%s for tariff with ID=%d".formatted(newRate,tariffId), ex);
    }

    public Tariff getTariffById(long tariffId) {
        return tariffRepository.findById(tariffId)
                .orElseThrow(() -> new EntityNotFoundException("There is no tariff with ID=%d found in database.".formatted(tariffId)));
    }

    public void deleteTariff(long tariffId) {
        tariffRepository.deleteById(tariffId);
    }

    private void handleDataIntegrityViolationException(DataIntegrityViolationException ex, String tariffName) {
        if (ex.getMessage().contains(TARIFF_NAME_UNIQUE_CONSTRAINT)) {
            String exceptionMessage = "Unable to set tariff name='%s': there is already existing tariff with this name."
                    .formatted(tariffName);
            throw new UniqueConstraintException(exceptionMessage, ex);
        }
        throw ex;
    }

    private TariffRateChangelog buildTariffRateChangelog(Tariff tariff, BigDecimal rate) {
        return TariffRateChangelog
                .builder()
                .tariff(tariff)
                .rate(rate)
                .build();
    }
}