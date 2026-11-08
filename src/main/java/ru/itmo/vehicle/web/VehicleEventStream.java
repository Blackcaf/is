package ru.itmo.vehicle.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.service.VehicleEventBus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class VehicleEventStream {
    private final VehicleEventBus eventBus;
    private final WebMapper mapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public VehicleEventStream(VehicleEventBus eventBus, WebMapper mapper) {
        this.eventBus = eventBus;
        this.mapper = mapper;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        VehicleEventBus.Listener listener = (action, vehicle) -> sendEvent(emitter, action, vehicle);
        Runnable cleanup = () -> {
            emitters.remove(emitter);
            eventBus.unsubscribe(listener);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        emitters.add(emitter);
        eventBus.subscribe(listener);
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException | IllegalStateException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, VehicleEventBus.Action action, Vehicle vehicle) {
        try {
            emitter.send(SseEmitter.event().name(action.name().toLowerCase(java.util.Locale.ROOT))
                    .data(Map.of("vehicle", mapper.toDto(vehicle))));
        } catch (IOException | IllegalStateException exception) {
            emitter.completeWithError(exception);
        }
    }
}
