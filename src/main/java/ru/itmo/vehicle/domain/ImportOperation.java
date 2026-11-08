package ru.itmo.vehicle.domain;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "import_operation",
        indexes = {
                @Index(name = "idx_import_operation_user", columnList = "user_id"),
                @Index(name = "idx_import_operation_created", columnList = "created_at")
        })
public class ImportOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImportStatus status;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "added_count")
    private Integer addedCount;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime createdAt;

    @Column(name = "completed_at")
    @Convert(converter = ZonedDateTimeConverter.class)
    private ZonedDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public void setStatus(ImportStatus status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(Integer addedCount) {
        this.addedCount = addedCount;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(ZonedDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
