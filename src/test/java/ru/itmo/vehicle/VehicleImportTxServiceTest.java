package ru.itmo.vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.itmo.vehicle.domain.ImportOperation;
import ru.itmo.vehicle.domain.ImportStatus;
import ru.itmo.vehicle.domain.Vehicle;
import ru.itmo.vehicle.exception.ValidationException;
import ru.itmo.vehicle.repo.CoordinatesRepository;
import ru.itmo.vehicle.repo.ImportOperationRepository;
import ru.itmo.vehicle.repo.VehicleRepository;
import ru.itmo.vehicle.service.importing.ImportResult;
import ru.itmo.vehicle.service.importing.VehicleImportRow;
import ru.itmo.vehicle.service.importing.VehicleImportTxService;

import javax.validation.Validation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleImportTxServiceTest {
    private VehicleRepository vehicleRepository;
    private CoordinatesRepository coordinatesRepository;
    private ImportOperationRepository operationRepository;
    private VehicleImportTxService service;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        coordinatesRepository = mock(CoordinatesRepository.class);
        operationRepository = mock(ImportOperationRepository.class);
        service = new VehicleImportTxService(vehicleRepository, coordinatesRepository, operationRepository,
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    void anyInvalidRowMeansNoObjectsAreSaved() {
        when(vehicleRepository.existsByNameIgnoreCase(any(), any())).thenReturn(false);
        when(coordinatesRepository.findByPair(any(), anyInt())).thenReturn(Optional.empty());

        List<VehicleImportRow> rows = List.of(
                row(2, "Valid", "1", "2", "10", "2", "100", "5"),
                row(3, "Invalid", "1", "2", "-10", "2", "100", "5")
        );

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.importRows(rows, 1L));

        assertTrue(exception.getFieldErrors().containsKey("row 3.enginePower"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(vehicleRepository, never()).flush();
    }

    @Test
    void validRowsAreSavedAndOperationBecomesSuccessful() {
        when(vehicleRepository.existsByNameIgnoreCase(any(), any())).thenReturn(false);
        when(coordinatesRepository.findByPair(any(), anyInt())).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ImportOperation operation = new ImportOperation();
        operation.setId(5L);
        operation.setStatus(ImportStatus.RUNNING);
        when(operationRepository.findById(5L)).thenReturn(Optional.of(operation));

        ImportResult result = service.importRows(List.of(
                row(2, "First", "1", "2", "10", "2", "100", "5"),
                row(3, "Second", "3", "4", "20", "3", "200", "6")
        ), 5L);

        assertEquals(2, result.addedCount());
        assertEquals("SUCCESS", result.status());
        assertEquals(ImportStatus.SUCCESS, operation.getStatus());
        assertEquals(2, operation.getAddedCount());
        verify(vehicleRepository, times(2)).save(any(Vehicle.class));
        verify(vehicleRepository).flush();
    }

    @Test
    void invalidNameDoesNotProduceSpuriousCoordinatesError() {
        when(vehicleRepository.existsByNameIgnoreCase(any(), any())).thenReturn(false);
        when(coordinatesRepository.findByPair(any(), anyInt())).thenReturn(Optional.empty());

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.importRows(List.of(
                        row(2, " ", "1", "2", "10", "2", "100", "5")), 1L));

        assertTrue(exception.getFieldErrors().containsKey("row 2.name"));
        assertFalse(exception.getFieldErrors().containsKey("row 2.coordinates"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void duplicateNamesInsideFileAreRejected() {        when(vehicleRepository.existsByNameIgnoreCase(any(), any())).thenReturn(false);
        when(coordinatesRepository.findByPair(any(), anyInt())).thenReturn(Optional.empty());

        List<VehicleImportRow> rows = List.of(
                row(2, "Same", "1", "2", "10", "2", "100", "5"),
                row(3, " same ", "3", "4", "20", "3", "200", "6")
        );

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.importRows(rows, 1L));
        assertTrue(exception.getFieldErrors().containsKey("row 3.name"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    private VehicleImportRow row(int number, String name, String x, String y, String enginePower,
                                 String capacity, String distance, String consumption) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("coordinates.x", x);
        values.put("coordinates.y", y);
        values.put("type", "DRONE");
        values.put("enginePower", enginePower);
        values.put("numberOfWheels", "4");
        values.put("capacity", capacity);
        values.put("distanceTravelled", distance);
        values.put("fuelConsumption", consumption);
        values.put("fuelType", "ELECTRICITY");
        return new VehicleImportRow(number, values);
    }
}
