package ru.itmo.vehicle.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.vehicle.service.VehicleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService service;
    private final WebMapper mapper;
    private final VehicleEventStream eventStream;

    public VehicleController(VehicleService service, WebMapper mapper, VehicleEventStream eventStream) {
        this.service = service;
        this.mapper = mapper;
        this.eventStream = eventStream;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto create(@RequestBody VehicleDto dto) {
        return mapper.toDto(service.create(mapper.toEntity(dto)));
    }

    @GetMapping("/{id}")
    public VehicleDto get(@PathVariable Long id) {
        return mapper.toDto(service.getRequired(id));
    }

    @PutMapping("/{id}")
    public VehicleDto update(@PathVariable Long id, @RequestBody VehicleDto dto) {
        return mapper.toDto(service.update(id, mapper.toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public Map<String, Long> delete(@PathVariable Long id,
                                    @RequestParam(required = false) Long version) {
        service.delete(id, version);
        return Map.of("deleted", id);
    }

    @GetMapping
    public VehiclePageDto list(@RequestParam(required = false) String filterField,
                               @RequestParam(required = false) String filterValue,
                               @RequestParam(required = false) String sortField,
                               @RequestParam(defaultValue = "true") boolean asc,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        List<VehicleDto> items = service.page(filterField, filterValue, sortField, asc, page, size)
                .stream().map(mapper::toDto).toList();
        return new VehiclePageDto(items, service.count(filterField, filterValue), page, size);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return eventStream.subscribe();
    }
}
