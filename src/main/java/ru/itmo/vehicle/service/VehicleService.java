package ru.itmo.vehicle.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.itmo.vehicle.domain.FuelType;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.domain.VehicleType;
import ru.itmo.vehicle.repo.VehicleRepository;
import ru.itmo.vehicle.repo.CoordinatesRepository;
import ru.itmo.vehicle.exception.ConflictException;
import ru.itmo.vehicle.exception.ResourceNotFoundException;
import ru.itmo.vehicle.exception.ValidationException;

import javax.validation.ConstraintViolation;
import javax.validation.groups.Default;
import javax.validation.Validator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Service
public class VehicleService {
    private final VehicleRepository repository;
    private final CoordinatesRepository coordinatesRepository;
    private final Validator validator;
    private final VehicleEventBus eventBus;

    public VehicleService(VehicleRepository repository, CoordinatesRepository coordinatesRepository,
                          Validator validator, VehicleEventBus eventBus) {
        this.repository = repository;
        this.coordinatesRepository = coordinatesRepository;
        this.validator = validator;
        this.eventBus = eventBus;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vehicle create(Vehicle vehicle) {
        normalize(vehicle);
        validate(vehicle);
        ensureNameUnique(vehicle.getName(), null);
        resolveCoordinates(vehicle);
        vehicle.setId(null);
        vehicle.setCreationDate(null);
        vehicle.setVersion(null);
        Vehicle saved = repository.save(vehicle);
        repository.flush();
        publishAfterCommit(VehicleEventBus.Action.CREATED, saved);
        return saved;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Vehicle getRequired(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle with id=" + id + " not found"));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Vehicle> getById(Long id) {
        return repository.findById(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vehicle update(Long id, Vehicle changes) {
        Vehicle existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Транспортное средство с id=" + id + " не найдено"));
        if (changes.getVersion() != null && !changes.getVersion().equals(existing.getVersion())) {
            throw new ConflictException(Map.of("general",
                    "Объект уже изменён другим пользователем. Обновите данные и повторите редактирование."));
        }
        normalize(changes);
        changes.setId(existing.getId());
        changes.setVersion(existing.getVersion());
        changes.setCreationDate(existing.getCreationDate());
        validate(changes);
        ensureNameUnique(changes.getName(), id);
        resolveCoordinates(changes);
        Vehicle saved = repository.save(changes);
        repository.flush();
        publishAfterCommit(VehicleEventBus.Action.UPDATED, saved);
        return saved;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(Long id) {
        deleteInternal(id, null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(Long id, Long expectedVersion) {
        deleteInternal(id, expectedVersion);
    }

    private void deleteInternal(Long id, Long expectedVersion) {
        Vehicle v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Транспортное средство с id=" + id + " не найдено"));
        if (expectedVersion != null && !expectedVersion.equals(v.getVersion())) {
            throw new ConflictException(Map.of("general",
                    "Объект уже изменён другим пользователем. Удаление отменено."));
        }
        repository.deleteById(id);
        repository.flush();
        publishAfterCommit(VehicleEventBus.Action.DELETED, v);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Vehicle> page(String filterField, String filterValue, String sortField, boolean asc,
                              int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        return repository.findPage(filterField, filterValue, sortField, asc, (p - 1) * s, s);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public long count(String filterField, String filterValue) {
        return repository.countFiltered(filterField, filterValue);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public double sumFuelConsumption() {
        return repository.sumFuelConsumption();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Vehicle minByTypeRequired() {
        return minByType().orElseThrow(() -> new ResourceNotFoundException("No vehicle with a non-null type was found"));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Vehicle> minByType() {
        List<Vehicle> all = repository.findAll();
        Vehicle best = null;
        for (Vehicle v : all) {
            if (v.getType() == null) continue;
            if (best == null || v.getType().ordinal() < best.getType().ordinal()) {
                best = v;
            }
        }
        return Optional.ofNullable(best);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public long countFuelTypeGreaterThan(String fuelType) {
        try {
            return countFuelTypeGreaterThan(FuelType.valueOf(fuelType.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ValidationException(Map.of("fuelType", "Invalid fuel type: " + fuelType));
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public long countFuelTypeGreaterThan(FuelType fuelType) {
        if (fuelType == null) {
            throw new ValidationException(Map.of("fuelType", "Не задан тип топлива для сравнения"));
        }
        long count = 0;
        for (Vehicle v : repository.findAll()) {
            if (v.getFuelType() != null && v.getFuelType().ordinal() > fuelType.ordinal()) {
                count++;
            }
        }
        return count;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Vehicle> findByType(String type) {
        try {
            return findByType(VehicleType.valueOf(type.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ValidationException(Map.of("type", "Invalid vehicle type: " + type));
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Vehicle> findByType(VehicleType type) {
        if (type == null) {
            throw new ValidationException(Map.of("type", "Не задан тип транспортного средства"));
        }
        return repository.findByType(type);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vehicle addWheels(Long id, long wheelsToAdd) {
        if (wheelsToAdd <= 0) {
            throw new ValidationException(Map.of("wheelsToAdd", "Число добавляемых колёс должно быть больше 0"));
        }
        Vehicle v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Транспортное средство с id=" + id + " не найдено"));
        long current = v.getNumberOfWheels() == null ? 0L : v.getNumberOfWheels();
        v.setNumberOfWheels(current + wheelsToAdd);
        Vehicle saved = repository.save(v);
        publishAfterCommit(VehicleEventBus.Action.UPDATED, saved);
        return saved;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteCoordinates(Long coordinatesId, Long replacementId) {
        Coordinates oldCoordinates = coordinatesRepository.findById(coordinatesId)
                .orElseThrow(() -> new ResourceNotFoundException("Coordinates с id=" + coordinatesId + " не найден"));
        Coordinates replacement = coordinatesRepository.findById(replacementId)
                .orElseThrow(() -> new ResourceNotFoundException("Coordinates с id=" + replacementId + " не найден"));
        if (coordinatesId.equals(replacementId)) {
            throw new ValidationException(Map.of("replacementId", "Нужно выбрать другой объект Coordinates"));
        }
        for (Vehicle vehicle : repository.findByCoordinatesId(coordinatesId)) {
            vehicle.setCoordinates(replacement);
            repository.save(vehicle);
            publishAfterCommit(VehicleEventBus.Action.UPDATED, vehicle);
        }
        coordinatesRepository.delete(oldCoordinates);
    }

    public void validate(Vehicle vehicle) {
        Set<ConstraintViolation<Vehicle>> violations = validator.validate(vehicle, Default.class);
        if (vehicle.getId() == null) {
            violations.removeIf(cv -> {
                String path = cv.getPropertyPath().toString();
                return path.equals("creationDate") || path.equals("id");
            });
        }

        if (!violations.isEmpty()) {
            Map<String, String> errors = new TreeMap<>();
            for (ConstraintViolation<Vehicle> cv : violations) {
                errors.putIfAbsent(cv.getPropertyPath().toString(), cv.getMessage());
            }
            throw new ValidationException(errors);
        }
        if (vehicle.getName() == null || vehicle.getName().isBlank()) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("name", "Поле name не может быть null, строка не может быть пустой");
            throw new ValidationException(errors);
        }
        if (vehicle.getCoordinates() == null) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("coordinates", "Поле coordinates не может быть null");
            throw new ValidationException(errors);
        }
    }

    private void normalize(Vehicle vehicle) {
        if (vehicle.getName() != null && !vehicle.getName().isBlank()) {
            vehicle.setName(vehicle.getName().trim());
        }
    }

    private void ensureNameUnique(String name, Long excludeId) {
        if (repository.existsByNameIgnoreCase(name, excludeId)) {
            throw new ConflictException(Map.of("name",
                    "Транспортное средство с названием «" + name + "» уже существует"));
        }
    }

    private void resolveCoordinates(Vehicle vehicle) {
        Coordinates coordinates = vehicle.getCoordinates();
        if (coordinates == null) {
            return;
        }
        if (coordinates.getId() != null) {
            Coordinates existing = coordinatesRepository.findById(coordinates.getId())
                    .orElseThrow(() -> new ValidationException(
                            Map.of("coordinates", "Выбранный объект Coordinates не найден")));
            vehicle.setCoordinates(existing);
            return;
        }
        if (coordinates.getX() == null) {
            return;
        }
        coordinatesRepository.findByPair(coordinates.getX(), coordinates.getY())
                .ifPresent(vehicle::setCoordinates);
    }

    private void publishAfterCommit(VehicleEventBus.Action action, Vehicle vehicle) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventBus.publish(action, copyOf(vehicle));
                }
            });
        } else {
            eventBus.publish(action, copyOf(vehicle));
        }
    }

    static Vehicle copyOf(Vehicle src) {
        Vehicle c = new Vehicle();
        c.setId(src.getId());
        c.setName(src.getName());
        if (src.getCoordinates() != null) {
            c.setCoordinates(new ru.itmo.vehicle.domain.Coordinates(src.getCoordinates().getX(),
                    src.getCoordinates().getY()));
            c.getCoordinates().setId(src.getCoordinates().getId());
        }
        c.setCreationDate(src.getCreationDate());
        c.setType(src.getType());
        c.setEnginePower(src.getEnginePower());
        c.setNumberOfWheels(src.getNumberOfWheels());
        c.setCapacity(src.getCapacity());
        c.setDistanceTravelled(src.getDistanceTravelled());
        c.setFuelConsumption(src.getFuelConsumption());
        c.setFuelType(src.getFuelType());
        c.setVersion(src.getVersion());
        return c;
    }
}
