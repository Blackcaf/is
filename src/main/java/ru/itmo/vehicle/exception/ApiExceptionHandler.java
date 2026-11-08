package ru.itmo.vehicle.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ImportFailedException.class)
    public ResponseEntity<ApiError> handleImportFailed(ImportFailedException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                exception.getFieldErrors(), exception.getOperationId(), "FAILED", exception.getTotalRows()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getFieldErrors()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getFieldErrors()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.general(exception.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.general(exception.getMessage()));
    }

    @ExceptionHandler({ConcurrencyFailureException.class, ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class, PessimisticLockException.class})
    public ResponseEntity<ApiError> handleConcurrentModification(Exception exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.general(
                "Объект уже изменён другим пользователем или транзакция была прервана из-за конфликта. "
                        + "Обновите данные и повторите операцию."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.general(
                "Нарушено ограничение целостности данных. Обновите данные и повторите операцию."));
    }

    @ExceptionHandler({PersistenceException.class, DatabaseException.class, TransactionSystemException.class})
    public ResponseEntity<ApiError> handlePersistenceFailure(RuntimeException exception) {
        if (isTransactionConflict(exception)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.general(
                    "Транзакция прервана из-за конфликта с параллельным изменением. "
                            + "Обновите данные и повторите операцию."));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.general("Ошибка при работе с базой данных"));
    }

    private boolean isTransactionConflict(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if ("40001".equals(sqlState) || "40P01".equals(sqlState)) {
                    return true;
                }
            }
            if (current instanceof DatabaseException databaseException) {
                if (databaseException.getDatabaseErrorCode() == 40001) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        String message = exception.getMessage() == null ? "Некорректный формат запроса" : exception.getMessage();
        return ResponseEntity.badRequest().body(ApiError.general(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiError.general(
                "Файл слишком большой. Максимальный размер файла — 5 МБ."));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiError(Map<String, String> errors, Long operationId, String status, Integer totalRows) {
        public ApiError(Map<String, String> errors) {
            this(errors, null, null, null);
        }

        static ApiError general(String message) {
            return new ApiError(Map.of("general", message));
        }
    }
}
