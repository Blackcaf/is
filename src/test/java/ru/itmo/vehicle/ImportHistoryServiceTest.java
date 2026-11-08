package ru.itmo.vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.itmo.vehicle.domain.UserRole;
import ru.itmo.vehicle.repo.AppUserRepository;
import ru.itmo.vehicle.repo.ImportOperationRepository;
import ru.itmo.vehicle.domain.UserPrincipal;
import ru.itmo.vehicle.service.importing.ImportHistoryPage;
import ru.itmo.vehicle.service.importing.ImportHistoryService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ImportHistoryServiceTest {
    private ImportOperationRepository operationRepository;
    private ImportHistoryService service;

    @BeforeEach
    void setUp() {
        operationRepository = mock(ImportOperationRepository.class);
        service = new ImportHistoryService(operationRepository, mock(AppUserRepository.class));
    }

    @Test
    void userSeesOnlyOwnOperations() {
        UserPrincipal user = new UserPrincipal(7L, "user", UserRole.USER);
        when(operationRepository.findPage(7L, 0, 20)).thenReturn(List.of());
        when(operationRepository.count(7L)).thenReturn(3L);

        ImportHistoryPage result = service.page(user, 1, 20);

        assertEquals(3L, result.total());
        verify(operationRepository).findPage(7L, 0, 20);
        verify(operationRepository).count(7L);
    }

    @Test
    void adminSeesOperationsOfAllUsers() {
        UserPrincipal admin = new UserPrincipal(1L, "admin", UserRole.ADMIN);
        UserPrincipal user = new UserPrincipal(7L, "user", UserRole.USER);
        when(operationRepository.findPage(null, 0, 20)).thenReturn(List.of());
        when(operationRepository.count(null)).thenReturn(5L);

        ImportHistoryPage result = service.page(admin, 1, 20);

        assertEquals(5L, result.total());
        verify(operationRepository).findPage(null, 0, 20);
        verify(operationRepository).count(null);
        service.page(user, 1, 20);
        verify(operationRepository).findPage(7L, 0, 20);
    }
}
