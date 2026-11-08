package ru.itmo.vehicle.service.importing;

import ru.itmo.vehicle.domain.ImportOperation;

import java.util.List;

public record ImportHistoryPage(List<ImportOperation> items, long total, int page, int size) {
}
