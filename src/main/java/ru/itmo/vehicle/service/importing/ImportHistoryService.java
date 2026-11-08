package ru.itmo.vehicle.service.importing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicle.domain.AppUser;
import ru.itmo.vehicle.domain.ImportOperation;
import ru.itmo.vehicle.domain.ImportStatus;
import ru.itmo.vehicle.exception.UnauthorizedException;
import ru.itmo.vehicle.repo.AppUserRepository;
import ru.itmo.vehicle.repo.ImportOperationRepository;
import ru.itmo.vehicle.domain.UserPrincipal;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class ImportHistoryService {
    private static final int MAX_ERROR_LENGTH = 3900;

    private final ImportOperationRepository operationRepository;
    private final AppUserRepository userRepository;

    public ImportHistoryService(ImportOperationRepository operationRepository, AppUserRepository userRepository) {
        this.operationRepository = operationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public ImportOperation start(UserPrincipal principal, String fileName) {
        AppUser user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден"));
        ImportOperation operation = new ImportOperation();
        operation.setUser(user);
        operation.setStatus(ImportStatus.RUNNING);
        operation.setFileName(truncate(fileName, 500));
        return operationRepository.save(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void markFailed(Long operationId, String errorMessage, Integer totalRows) {
        operationRepository.findById(operationId).ifPresent(operation -> {
            operation.setStatus(ImportStatus.FAILED);
            operation.setAddedCount(null);
            operation.setTotalRows(totalRows);
            operation.setErrorMessage(truncate(errorMessage, MAX_ERROR_LENGTH));
            operation.setCompletedAt(ZonedDateTime.now());
            operationRepository.save(operation);
        });
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ImportHistoryPage page(UserPrincipal principal, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Long userId = principal.isAdmin() ? null : principal.id();
        List<ImportOperation> items = operationRepository.findPage(userId, (p - 1) * s, s);
        return new ImportHistoryPage(items, operationRepository.count(userId), p, s);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
