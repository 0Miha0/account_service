package faang.school.accountservice.service.tariff;

import faang.school.accountservice.dto.tariff.TariffCreateDto;
import faang.school.accountservice.dto.tariff.TariffResponse;
import faang.school.accountservice.entity.tariff.Tariff;
import faang.school.accountservice.exception.UniqueConstraintException;
import faang.school.accountservice.mapper.tariff.TariffMapper;
import faang.school.accountservice.repository.tariff.TariffRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @Spy
    TariffMapper tariffMapper = Mappers.getMapper(TariffMapper.class);

    @InjectMocks
    TariffService tariffService;

    @Test
    void createTariffValidTest() {
        String tariffName = "bonus";
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);
        TariffCreateDto createDto = TariffCreateDto.builder()
                .name(tariffName)
                .rate(tariffRate)
                .build();

        when(tariffRepository.save(any())).thenReturn(Tariff.builder()
                .name(tariffName)
                .currentRate(tariffRate)
                .build()
        );

        TariffResponse response = tariffService.createTariff(createDto);

        assertEquals(tariffName, response.getName());
        assertEquals(tariffRate, response.getCurrentRate());
        verify(tariffRepository, times(1)).save(any());
        verify(tariffMapper, times(1)).toResponse(any());
        verify(tariffMapper, times(1)).toEntity(any());
    }

    @Test
    void createTariffAlreadyExistingNameTest() {
        String tariffName = "bonus";
        BigDecimal tariffRate = BigDecimal.valueOf(10.5);
        TariffCreateDto createDto = TariffCreateDto.builder()
                .name(tariffName)
                .rate(tariffRate)
                .build();

        when(tariffRepository.save(any())).thenThrow(new DataIntegrityViolationException("constraint [tariff_name_key]"));

        assertThrows(UniqueConstraintException.class, () -> tariffService.createTariff(createDto));

        verify(tariffRepository, times(1)).save(any());
        verify(tariffMapper, never()).toResponse(any());
    }

    @Test
    void updateNotExistingTariffRateTest() {
        long tariffId = 1L;
        BigDecimal newRate = BigDecimal.valueOf(10.5);
        when(tariffRepository.findById(tariffId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> tariffService.updateTariffRate(tariffId, newRate));
    }

    @Test
    void updateTariffRateTest() {
        long tariffId = 1L;
        String tariffName = "bonus";
        BigDecimal newRate = BigDecimal.valueOf(11);
        Tariff tariff = new Tariff();
        tariff.setRateChangelogs(new ArrayList<>());
        tariff.setName(tariffName);

        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
        when(tariffRepository.save(tariff)).thenReturn(tariff);

        TariffResponse response = assertDoesNotThrow(() -> tariffService.updateTariffRate(tariffId, newRate));

        assertEquals(tariffName, response.getName());
        verify(tariffRepository, times(1)).save(tariff);
        verify(tariffRepository, times(1)).findById(tariffId);
    }

    @Test
    void getAllTariffsTest() {
        when(tariffRepository.findAll()).thenReturn(List.of(new Tariff(), new Tariff()));
        List<TariffResponse> tariffs = tariffService.getAllTariffs();
        assertEquals(2, tariffs.size());
        verify(tariffRepository, times(1)).findAll();
    }

    @Test
    void deleteTariffTest() {
        long tariffId = 1L;
        assertDoesNotThrow(() -> tariffService.deleteTariff(tariffId));
        verify(tariffRepository, times(1)).deleteById(tariffId);
    }

    @Test
    void getExistingTariffByIdTest() {
        long tariffId = 1L;
        Tariff tariff = new Tariff();
        tariff.setId(tariffId);
        when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));

        Tariff resultTariff = assertDoesNotThrow(() -> tariffService.getTariffById(tariffId));

        assertEquals(tariffId, resultTariff.getId());
        verify(tariffRepository, times(1)).findById(tariffId);
    }

    @Test
    void getTariffByIdNotFoundTest() {
        long tariffId = 1L;
        when(tariffRepository.findById(tariffId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> tariffService.getTariffById(tariffId));

        verify(tariffRepository, times(1)).findById(tariffId);
    }
}