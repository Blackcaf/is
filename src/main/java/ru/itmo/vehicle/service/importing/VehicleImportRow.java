package ru.itmo.vehicle.service.importing;

import java.util.Map;

public record VehicleImportRow(int rowNumber, Map<String, String> values) {
    public String get(String field) {
        return values.get(field);
    }
}
