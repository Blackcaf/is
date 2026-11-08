package ru.itmo.vehicle.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicle.domain.Coordinates;
import ru.itmo.vehicle.repo.CoordinatesRepository;
import ru.itmo.vehicle.exception.ConflictException;
import ru.itmo.vehicle.exception.ValidationException;

import java.util.List;
import java.util.Map;

@Service
public class CoordinatesService {
    private final CoordinatesRepository repository;

    public CoordinatesService(CoordinatesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Coordinates> findAll() {
        return repository.findAll();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Coordinates create(Coordinates coordinates) {
        if (coordinates.getX() == null) {
            throw new ValidationException(java.util.Map.of("x", "Поле x не может быть null"));
        }
        repository.findByPair(coordinates.getX(), coordinates.getY()).ifPresent(existing -> {
            throw new ConflictException(Map.of("coordinates",
                    "Coordinates с такими координатами уже существует (id=" + existing.getId() + ")"));
        });
        return repository.save(coordinates);
    }
}
