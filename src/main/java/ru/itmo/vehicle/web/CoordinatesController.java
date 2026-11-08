package ru.itmo.vehicle.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.vehicle.service.CoordinatesService;
import ru.itmo.vehicle.service.VehicleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinatesController {
    private final CoordinatesService coordinatesService;
    private final VehicleService vehicleService;
    private final WebMapper mapper;

    public CoordinatesController(CoordinatesService coordinatesService, VehicleService vehicleService,
                                 WebMapper mapper) {
        this.coordinatesService = coordinatesService;
        this.vehicleService = vehicleService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CoordinatesDto> list() {
        return coordinatesService.findAll().stream().map(mapper::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoordinatesDto create(@RequestBody CoordinatesDto dto) {
        return mapper.toDto(coordinatesService.create(mapper.toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public Map<String, Long> delete(@PathVariable Long id, @RequestParam Long replacementId) {
        vehicleService.deleteCoordinates(id, replacementId);
        return Map.of("deleted", id, "replacementId", replacementId);
    }
}
