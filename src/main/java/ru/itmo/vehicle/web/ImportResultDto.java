package ru.itmo.vehicle.web;

public record ImportResultDto(Long operationId, String status, Integer addedCount, Integer totalRows) {
}
