package ru.itmo.vehicle.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.vehicle.domain.UserPrincipal;
import ru.itmo.vehicle.service.importing.ImportHistoryPage;
import ru.itmo.vehicle.service.importing.ImportHistoryService;
import ru.itmo.vehicle.service.importing.ImportService;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportService importService;
    private final ImportHistoryService historyService;
    private final WebMapper mapper;

    public ImportController(ImportService importService, ImportHistoryService historyService, WebMapper mapper) {
        this.importService = importService;
        this.historyService = historyService;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResultDto upload(@RequestParam("file") MultipartFile file,
                                  @SessionAttribute(AuthInterceptor.SESSION_USER) UserPrincipal principal) {
        return mapper.toDto(importService.importFile(file, principal));
    }

    @GetMapping
    public ImportPageDto history(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @SessionAttribute(AuthInterceptor.SESSION_USER) UserPrincipal principal) {
        ImportHistoryPage result = historyService.page(principal, page, size);
        return new ImportPageDto(result.items().stream().map(mapper::toDto).toList(),
                result.total(), result.page(), result.size());
    }
}
