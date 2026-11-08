package ru.itmo.vehicle.exception;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImportFailedException extends ValidationException {
    private final Long operationId;
    private final Integer totalRows;

    public ImportFailedException(Long operationId, Integer totalRows, Map<String, String> fieldErrors) {
        super(withGeneral(fieldErrors));
        this.operationId = operationId;
        this.totalRows = totalRows;
    }

    public Long getOperationId() {
        return operationId;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    private static Map<String, String> withGeneral(Map<String, String> fieldErrors) {
        Map<String, String> result = new LinkedHashMap<>(fieldErrors);
        result.putIfAbsent("general", "Импорт не выполнен: ни один объект не был добавлен");
        return result;
    }
}
