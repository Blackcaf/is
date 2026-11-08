package ru.itmo.vehicle.service.importing;

public record ImportResult(Long operationId, String status, Integer addedCount, Integer totalRows) {
}
