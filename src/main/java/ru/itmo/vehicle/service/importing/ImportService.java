package ru.itmo.vehicle.service.importing;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.vehicle.domain.ImportOperation;
import ru.itmo.vehicle.exception.ConflictException;
import ru.itmo.vehicle.exception.ImportFailedException;
import ru.itmo.vehicle.exception.ValidationException;
import ru.itmo.vehicle.domain.UserPrincipal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ImportService {
    private final CsvVehicleParser parser;
    private final VehicleImportTxService importTxService;
    private final ImportHistoryService historyService;

    public ImportService(CsvVehicleParser parser,
                         VehicleImportTxService importTxService,
                         ImportHistoryService historyService) {
        this.parser = parser;
        this.importTxService = importTxService;
        this.historyService = historyService;
    }

    public ImportResult importFile(MultipartFile file, UserPrincipal principal) {
        String fileName = file == null ? null : file.getOriginalFilename();
        ImportOperation operation = historyService.start(principal, fileName);
        Integer totalRows = null;

        try {
            validateFile(file);
            List<VehicleImportRow> rows;
            try (InputStream inputStream = file.getInputStream()) {
                rows = parser.parse(inputStream);
            } catch (IOException exception) {
                throw new ValidationException(Map.of("file", "Не удалось прочитать файл: " + exception.getMessage()));
            }
            totalRows = rows.size();
            return importTxService.importRows(rows, operation.getId());
        } catch (ValidationException exception) {
            historyService.markFailed(operation.getId(), exception.getMessage(), totalRows);
            throw new ImportFailedException(operation.getId(), totalRows, exception.getFieldErrors());
        } catch (ConflictException exception) {
            historyService.markFailed(operation.getId(), exception.getMessage(), totalRows);
            throw new ImportFailedException(operation.getId(), totalRows, exception.getFieldErrors());
        } catch (RuntimeException exception) {
            historyService.markFailed(operation.getId(), exception.getMessage(), totalRows);
            throw new ImportFailedException(operation.getId(), totalRows,
                    Map.of("general", "Ошибка импорта: " + safeMessage(exception)));
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(Map.of("file", "Файл не выбран или пуст"));
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ValidationException(Map.of("file", "Поддерживаются только файлы формата .csv"));
        }
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
