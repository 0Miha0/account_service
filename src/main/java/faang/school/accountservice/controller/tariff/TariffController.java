package faang.school.accountservice.controller.tariff;

import faang.school.accountservice.dto.tariff.TariffCreateDto;
import faang.school.accountservice.dto.tariff.TariffResponse;
import faang.school.accountservice.service.tariff.TariffService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tariffs")
@RequiredArgsConstructor
@Validated
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffResponse createTariff(@Valid @RequestBody TariffCreateDto creationDto) {
        return tariffService.createTariff(creationDto);
    }

    @PatchMapping("/{tariffId}/rates")
    public TariffResponse updateTariffRate(
            @PathVariable @Min(1) long tariffId,
            @RequestParam @DecimalMin("0.01") @DecimalMax("99.9") @Digits(integer = 2, fraction = 2) BigDecimal newRate
    ) {
        return tariffService.updateTariffRate(tariffId, newRate);
    }

    @GetMapping
    public List<TariffResponse> getAllTariffs() {
        return tariffService.getAllTariffs();
    }

    @DeleteMapping("/{tariffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTariff(@PathVariable @Min(1) long tariffId) {
        tariffService.deleteTariff(tariffId);
    }
}
