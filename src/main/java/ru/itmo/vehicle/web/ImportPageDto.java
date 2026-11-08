package ru.itmo.vehicle.web;

import java.util.List;

public record ImportPageDto(List<ImportOperationDto> items, long total, int page, int size) {
}
