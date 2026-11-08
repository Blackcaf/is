package ru.itmo.vehicle.web;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.domain.ImportOperation;
import ru.itmo.vehicle.domain.UserPrincipal;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.service.importing.ImportResult;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WebMapper {
    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    VehicleDto toDto(Vehicle vehicle);

    Vehicle toEntity(VehicleDto dto);

    CoordinatesDto toDto(Coordinates coordinates);

    Coordinates toEntity(CoordinatesDto dto);

    UserDto toDto(UserPrincipal principal);

    ImportResultDto toDto(ImportResult result);

    @Mapping(target = "status",
            expression = "java(operation.getStatus() == null ? null : operation.getStatus().name())")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "addedCount",
            expression = "java(operation.getStatus() == ru.itmo.vehicle.domain.ImportStatus.SUCCESS"
                    + " ? operation.getAddedCount() : null)")
    @Mapping(target = "createdAt", expression = "java(format(operation.getCreatedAt()))")
    @Mapping(target = "completedAt", expression = "java(format(operation.getCompletedAt()))")
    ImportOperationDto toDto(ImportOperation operation);

    default String format(ZonedDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }
}
