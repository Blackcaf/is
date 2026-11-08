package ru.itmo.vehicle.exception;

import java.util.Map;

public class ConflictException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ConflictException(String message) {
        super(message);
        this.fieldErrors = Map.of("general", message);
    }

    public ConflictException(Map<String, String> fieldErrors) {
        super(fieldErrors.values().stream().findFirst().orElse("Конфликт данных"));
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
