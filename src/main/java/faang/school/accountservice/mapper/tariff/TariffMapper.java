package faang.school.accountservice.mapper.tariff;

import faang.school.accountservice.dto.tariff.TariffCreateDto;
import faang.school.accountservice.dto.tariff.TariffResponse;
import faang.school.accountservice.entity.tariff.Tariff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TariffMapper {

    TariffResponse toResponse(Tariff tariff);

    @Mapping(source = "rate", target = "currentRate")
    Tariff toEntity(TariffCreateDto createDto);
}
