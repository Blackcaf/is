package ru.itmo.vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.vehicle.domain.ImportOperation;
import ru.itmo.vehicle.exception.ImportFailedException;
import ru.itmo.vehicle.exception.ValidationException;
import ru.itmo.vehicle.domain.UserPrincipal;
import ru.itmo.vehicle.domain.UserRole;
import ru.itmo.vehicle.service.importing.CsvVehicleParser;
import ru.itmo.vehicle.service.importing.ImportHistoryService;
import ru.itmo.vehicle.service.importing.ImportService;
import ru.itmo.vehicle.service.importing.VehicleImportTxService;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class ImportServiceTest {
    private CsvVehicleParser parser;
    private VehicleImportTxService importTxService;
    private ImportHistoryService historyService;
    private ImportService service;

    private final UserPrincipal user = new UserPrincipal(7L, "user", UserRole.USER);

    @BeforeEach
    void setUp() {
        parser = mock(CsvVehicleParser.class);
        importTxService = mock(VehicleImportTxService.class);
        historyService = mock(ImportHistoryService.class);
        service = new ImportService(parser, importTxService, historyService);
    }

    @Test
    void parseErrorIsRecordedInHistoryAndNothingIsImported() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("broken.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(parser.parse(any())).thenThrow(new ValidationException(Map.of("file", "Файл пуст")));

        ImportOperation operation = new ImportOperation();
        operation.setId(42L);
        when(historyService.start(user, "broken.csv")).thenReturn(operation);

        ImportFailedException exception = assertThrows(ImportFailedException.class,
                () -> service.importFile(file, user));

        assertEquals(42L, exception.getOperationId());
        verify(historyService).markFailed(eq(42L), eq("Файл пуст"), isNull());
        verifyNoInteractions(importTxService);
    }

    @Test
    void nonCsvFileIsRejectedBeforeImport() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("data.txt");

        ImportOperation operation = new ImportOperation();
        operation.setId(43L);
        when(historyService.start(user, "data.txt")).thenReturn(operation);

        ImportFailedException exception = assertThrows(ImportFailedException.class,
                () -> service.importFile(file, user));

        assertEquals(43L, exception.getOperationId());
        verify(historyService).markFailed(eq(43L), any(), isNull());
        verifyNoInteractions(parser, importTxService);
    }
}
