package ru.itmo.vehicle.service.importing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.ImportOperation;
import ru.itmo.vehicle.domain.ImportStatus;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;
import ru.itmo.vehicle.exception.ValidationException;
import ru.itmo.vehicle.repo.CoordinatesRepository;
import ru.itmo.vehicle.repo.ImportOperationRepository;
import ru.itmo.vehicle.repo.VehicleRepository;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Service
public class VehicleImportTxService {
    private final VehicleRepository vehicleRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final ImportOperationRepository importOperationRepository;
    private final Validator validator;

    public VehicleImportTxService(VehicleRepository vehicleRepository,
                                  CoordinatesRepository coordinatesRepository,
                                  ImportOperationRepository importOperationRepository,
                                  Validator validator) {
        this.vehicleRepository = vehicleRepository;
        this.coordinatesRepository = coordinatesRepository;
        this.importOperationRepository = importOperationRepository;
        this.validator = validator;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ImportResult importRows(List<VehicleImportRow> rows, Long operationId) {
        vehicleRepository.setSerializableIsolation();
        Map<String, String> errors = new TreeMap<>(VehicleImportTxService::compareRowKeys);
        List<Vehicle> vehicles = new ArrayList<>();
        Map<String, Integer> namesInFile = new HashMap<>();
        Map<String, Coordinates> coordinatesInFile = new HashMap<>();

        for (VehicleImportRow row : rows) {
            Vehicle vehicle = mapRow(row, errors);
            if (vehicle == null) {
                continue;
            }

            String normalizedName = normalizeName(vehicle.getName());
            Integer firstRow = namesInFile.putIfAbsent(normalizedName, row.rowNumber());
            if (firstRow != null) {
                errors.put("row " + row.rowNumber() + ".name",
                        "Название «" + vehicle.getName() + "» уже встречается в файле (строка " + firstRow + ")");
            } else if (vehicleRepository.existsByNameIgnoreCase(vehicle.getName(), null)) {
                errors.put("row " + row.rowNumber() + ".name",
                        "Транспортное средство с названием «" + vehicle.getName() + "» уже существует");
            }

            Coordinates coordinates = vehicle.getCoordinates();
            if (coordinates != null && coordinates.getX() != null) {
                String pairKey = coordinates.getX() + "|" + coordinates.getY();
                Coordinates fromFile = coordinatesInFile.get(pairKey);
                if (fromFile != null) {
                    vehicle.setCoordinates(fromFile);
                } else {
                    Optional<Coordinates> fromDb = coordinatesRepository.findByPair(coordinates.getX(), coordinates.getY());
                    if (fromDb.isPresent()) {
                        vehicle.setCoordinates(fromDb.get());
                    } else {
                        coordinatesInFile.put(pairKey, coordinates);
                    }
                }
            }
            vehicles.add(vehicle);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        for (Vehicle vehicle : vehicles) {
            vehicle.setId(null);
            vehicle.setCreationDate(null);
            vehicle.setVersion(null);
            vehicleRepository.save(vehicle);
        }
        vehicleRepository.flush();

        ImportOperation operation = importOperationRepository.findById(operationId)
                .orElseThrow(() -> new ValidationException(Map.of("general", "Операция импорта не найдена")));
        operation.setStatus(ImportStatus.SUCCESS);
        operation.setAddedCount(vehicles.size());
        operation.setTotalRows(rows.size());
        operation.setCompletedAt(ZonedDateTime.now());
        operation.setErrorMessage(null);
        importOperationRepository.save(operation);

        return new ImportResult(operationId, ImportStatus.SUCCESS.name(), vehicles.size(), rows.size());
    }

    private Vehicle mapRow(VehicleImportRow row, Map<String, String> errors) {
        int rowNumber = row.rowNumber();
        String prefix = "row " + rowNumber + ".";
        Vehicle vehicle = new Vehicle();
        boolean rowHasError = false;

        String name = value(row, "name");
        if (name == null || name.isBlank()) {
            errors.put(prefix + "name", "Поле name не может быть null, строка не может быть пустой");
            rowHasError = true;
        } else {
            try {
                vehicle.setName(name.trim());
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "name", exception.getMessage());
                rowHasError = true;
            }
        }

        Double x = parseDouble(row, "coordinates.x", prefix, errors, true);
        Integer y = parseInteger(row, "coordinates.y", prefix, errors, true);
        boolean coordinatesValid = x != null && y != null;
        if (x != null && x > 862) {
            errors.put(prefix + "coordinates.x", "Максимальное значение поля x: 862");
            coordinatesValid = false;
        }
        if (y != null && y <= -860) {
            errors.put(prefix + "coordinates.y", "Значение поля y должно быть больше -860");
            coordinatesValid = false;
        }
        if (coordinatesValid) {
            try {
                vehicle.setCoordinates(new Coordinates(x, y));
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "coordinates", exception.getMessage());
                coordinatesValid = false;
            }
        }
        rowHasError |= !coordinatesValid;

        Double enginePower = parseDouble(row, "enginePower", prefix, errors, true);
        if (enginePower != null) {
            try {
                vehicle.setEnginePower(enginePower);
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "enginePower", exception.getMessage());
                rowHasError = true;
            }
        } else {
            rowHasError = true;
        }

        Long numberOfWheels = parseLong(row, "numberOfWheels", prefix, errors, false);
        if (numberOfWheels != null) {
            try {
                vehicle.setNumberOfWheels(numberOfWheels);
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "numberOfWheels", exception.getMessage());
                rowHasError = true;
            }
        }

        Long capacity = parseLong(row, "capacity", prefix, errors, true);
        if (capacity != null) {
            try {
                vehicle.setCapacity(capacity);
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "capacity", exception.getMessage());
                rowHasError = true;
            }
        } else {
            rowHasError = true;
        }

        Float distanceTravelled = parseFloat(row, "distanceTravelled", prefix, errors, true);
        if (distanceTravelled != null) {
            try {
                vehicle.setDistanceTravelled(distanceTravelled);
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "distanceTravelled", exception.getMessage());
                rowHasError = true;
            }
        } else {
            rowHasError = true;
        }

        Double fuelConsumption = parseDouble(row, "fuelConsumption", prefix, errors, true);
        if (fuelConsumption != null) {
            try {
                vehicle.setFuelConsumption(fuelConsumption);
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "fuelConsumption", exception.getMessage());
                rowHasError = true;
            }
        } else {
            rowHasError = true;
        }

        String type = value(row, "type");
        if (type != null && !type.isBlank()) {
            try {
                vehicle.setType(VehicleType.valueOf(type.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "type", "Недопустимое значение type: " + type);
                rowHasError = true;
            }
        }

        String fuelType = value(row, "fuelType");
        if (fuelType != null && !fuelType.isBlank()) {
            try {
                vehicle.setFuelType(FuelType.valueOf(fuelType.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                errors.put(prefix + "fuelType", "Недопустимое значение fuelType: " + fuelType);
                rowHasError = true;
            }
        }

        Set<ConstraintViolation<Vehicle>> violations = validator.validate(vehicle, Default.class);
        for (ConstraintViolation<Vehicle> violation : violations) {
            String path = violation.getPropertyPath().toString();
            if ("creationDate".equals(path) || "id".equals(path)) {
                continue;
            }
            errors.putIfAbsent(prefix + path, violation.getMessage());
            rowHasError = true;
        }

        return rowHasError ? null : vehicle;
    }

    private String value(VehicleImportRow row, String field) {
        String value = row.get(field);
        return value == null ? null : value.trim();
    }

    private Double parseDouble(VehicleImportRow row, String field, String prefix,
                               Map<String, String> errors, boolean required) {
        String value = value(row, field);
        if (value == null || value.isBlank()) {
            if (required) {
                errors.put(prefix + field, "Поле " + field + " обязательно");
            }
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                errors.put(prefix + field, "Число должно быть конечным: " + value);
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            errors.put(prefix + field, "Некорректное числовое значение: " + value);
            return null;
        }
    }

    private Integer parseInteger(VehicleImportRow row, String field, String prefix,
                                 Map<String, String> errors, boolean required) {
        String value = value(row, field);
        if (value == null || value.isBlank()) {
            if (required) {
                errors.put(prefix + field, "Поле " + field + " обязательно");
            }
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            errors.put(prefix + field, "Некорректное целое значение: " + value);
            return null;
        }
    }

    private Long parseLong(VehicleImportRow row, String field, String prefix,
                           Map<String, String> errors, boolean required) {
        String value = value(row, field);
        if (value == null || value.isBlank()) {
            if (required) {
                errors.put(prefix + field, "Поле " + field + " обязательно");
            }
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            errors.put(prefix + field, "Некорректное целое значение: " + value);
            return null;
        }
    }

    private Float parseFloat(VehicleImportRow row, String field, String prefix,
                             Map<String, String> errors, boolean required) {
        String value = value(row, field);
        if (value == null || value.isBlank()) {
            if (required) {
                errors.put(prefix + field, "Поле " + field + " обязательно");
            }
            return null;
        }
        try {
            float parsed = Float.parseFloat(value);
            if (!Float.isFinite(parsed)) {
                errors.put(prefix + field, "Число должно быть конечным: " + value);
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            errors.put(prefix + field, "Некорректное числовое значение: " + value);
            return null;
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static int compareRowKeys(String left, String right) {
        return left.compareTo(right);
    }
}
