package ru.itmo.vehicle;

import org.junit.jupiter.api.Test;
import ru.itmo.vehicle.domain.AppUser;
import ru.itmo.vehicle.domain.ImportOperation;
import ru.itmo.vehicle.domain.ImportStatus;
import ru.itmo.vehicle.domain.UserRole;
import ru.itmo.vehicle.web.ImportOperationDto;
import ru.itmo.vehicle.web.WebMapper;
import ru.itmo.vehicle.web.WebMapperImpl;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WebMapperTest {
    private final WebMapper mapper = new WebMapperImpl();

    @Test
    void successfulOperationShowsAddedCount() {
        ImportOperation operation = operation(ImportStatus.SUCCESS, 4);
        ImportOperationDto dto = mapper.toDto(operation);

        assertEquals("SUCCESS", dto.status);
        assertEquals("user", dto.username);
        assertEquals(4, dto.addedCount);
        assertNotNull(dto.createdAt);
    }

    @Test
    void failedOperationHidesAddedCount() {
        ImportOperation operation = operation(ImportStatus.FAILED, 4);
        ImportOperationDto dto = mapper.toDto(operation);

        assertEquals("FAILED", dto.status);
        assertNull(dto.addedCount);
    }

    private ImportOperation operation(ImportStatus status, int addedCount) {
        ImportOperation operation = new ImportOperation();
        operation.setId(1L);
        operation.setUser(new AppUser("user", "hash", UserRole.USER));
        operation.setStatus(status);
        operation.setAddedCount(addedCount);
        operation.setCreatedAt(ZonedDateTime.now());
        return operation;
    }
}
